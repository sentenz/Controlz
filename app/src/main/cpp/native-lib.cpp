//
// Created by Sentenz on 01.06.2020.
//
#include <iostream>
#include <thread>
#include <future>

#include <jni.h>
#include <string.h>
#include <cinttypes>
#include <cstring>
#include <android/log.h>

//#include <gmath.h>
//#include <gperf.h>
//#include <open62541.h>
#include "../../../../gen-libs/src/main/cpp/gperf/src/gperf.h"
#include "../../../../gen-libs/src/main/cpp/gmath/src/gmath.h"
#include "../../../../gen-libs/src/main/cpp/open62541/src/open62541.h"

// Android log function wrappers
static const char* kTAG = "native-lib";
#define LOGI(...) \
  ((void)__android_log_print(ANDROID_LOG_INFO, kTAG, __VA_ARGS__))
#define LOGW(...) \
  ((void)__android_log_print(ANDROID_LOG_WARN, kTAG, __VA_ARGS__))
#define LOGE(...) \
  ((void)__android_log_print(ANDROID_LOG_ERROR, kTAG, __VA_ARGS__))

/*
 * JNI Call
 */

static jobject g_obj;
static JavaVM* g_jvm = nullptr;

void jvmCallMethod(bool connected) {
    JNIEnv *env;

    __android_log_print(ANDROID_LOG_DEBUG, "jvmCallMethod", "Attaching");
    jint res = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = g_jvm->AttachCurrentThread(&env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return;
        }
    }

    jclass clazz = env->GetObjectClass(g_obj);
    jmethodID methodID = env->GetMethodID(clazz, "jniConnectCallback", "(Z)V" );
    env->CallVoidMethod(g_obj, methodID, connected);

    __android_log_print(ANDROID_LOG_DEBUG, "jvmCallMethod", "Not Detaching");
    res = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (res != JNI_EDETACHED) {
        int detach = g_jvm->DetachCurrentThread();
        LOGI("Detach result ==: %d", detach);
    }
}

void jvmCallMessageMethod(jstring message) {
    JNIEnv *env;

    __android_log_print(ANDROID_LOG_DEBUG, "jvmCallMessageMethod", "Attaching");
    jint res = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (res != JNI_OK) {
        res = g_jvm->AttachCurrentThread(&env, NULL);
        if (JNI_OK != res) {
            LOGE("Failed to AttachCurrentThread, ErrorCode = %d", res);
            return;
        }
    }

    jclass clazz = env->GetObjectClass(g_obj);
    jmethodID methodID = env->GetMethodID(clazz, "jniMessageCallback", "(Ljava/lang/String;)V");
    env->CallVoidMethod(g_obj, methodID, message);

    __android_log_print(ANDROID_LOG_DEBUG, "jvmCallMessageMethod", "Not Detaching");
    res = g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (res != JNI_EDETACHED) {
        int detach = g_jvm->DetachCurrentThread();
        LOGI("Detach result ==: %d", detach);
    }
}

/*
 * Initializer
 */

UA_Client * g_client = NULL;

#ifdef UA_ENABLE_SUBSCRIPTIONS
const size_t g_nSelectClauses = 2;
UA_CreateSubscriptionResponse g_response;
UA_MonitoredItemCreateResult g_result;
UA_EventFilter g_filter;
#endif /* UA_ENABLE_SUBSCRIPTIONS */

bool g_running = true;
bool g_connected = false;
bool g_event = false;

std::thread g_thread;

// TODO Destructor isn't called
class scoped_thread{
    std::thread t;
public:
    explicit scoped_thread() = default;
    explicit scoped_thread(std::thread t_) : t(std::move(t_)) {
        if (!t.joinable()) throw std::logic_error("No thread");
    }
    ~scoped_thread() {
        t.join();
    }
    scoped_thread(scoped_thread&) = delete;
    scoped_thread& operator=(scoped_thread const &) = delete;

    void add(std::thread t_) {
        t = std::move(t_);
    }
};

scoped_thread g_t;

/*
 * Event
 */

#ifdef UA_ENABLE_SUBSCRIPTIONS

static void
opcua_event_callback(UA_Client *client, UA_UInt32 subId, void *subContext,
                    UA_UInt32 monId, void *monContext,
                    size_t nEventFields, UA_Variant *eventFields) {
    UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "Notification");
    LOGI("opcua_event_callback");

    /* The context should point to the monId on the stack */
    //UA_assert(*(UA_UInt32*)monContext == monId);

    for(size_t i = 0; i < nEventFields; ++i) {
        if(UA_Variant_hasScalarType(&eventFields[i], &UA_TYPES[UA_TYPES_UINT16])) {
            UA_UInt16 severity = *(UA_UInt16 *)eventFields[i].data;
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "Severity: %u", severity);
        } else if (UA_Variant_hasScalarType(&eventFields[i], &UA_TYPES[UA_TYPES_LOCALIZEDTEXT])) {
            UA_LocalizedText *lt = (UA_LocalizedText *)eventFields[i].data;
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                        "Message: '%.*s'", (int)lt->text.length, lt->text.data);
        }
        else {
#ifdef UA_ENABLE_TYPEDESCRIPTION
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                        "Don't know how to handle type: '%s'", eventFields[i].type->typeName);
#else
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                        "Don't know how to handle type, enable UA_ENABLE_TYPEDESCRIPTION "
                        "for typename");
#endif
        }
    }
}

static UA_SimpleAttributeOperand *
opcua_event_select_clauses() {
    UA_SimpleAttributeOperand *selectClauses = (UA_SimpleAttributeOperand*)
            UA_Array_new(g_nSelectClauses, &UA_TYPES[UA_TYPES_SIMPLEATTRIBUTEOPERAND]);
    if(!selectClauses)
        return NULL;

    for(size_t i = 0; i < g_nSelectClauses; ++i) {
        UA_SimpleAttributeOperand_init(&selectClauses[i]);
    }

    selectClauses[0].typeDefinitionId = UA_NODEID_NUMERIC(0, UA_NS0ID_BASEEVENTTYPE);
    selectClauses[0].browsePathSize = 1;
    selectClauses[0].browsePath = (UA_QualifiedName*)
            UA_Array_new(selectClauses[0].browsePathSize, &UA_TYPES[UA_TYPES_QUALIFIEDNAME]);
    if(!selectClauses[0].browsePath) {
        UA_SimpleAttributeOperand_delete(selectClauses);
        return NULL;
    }
    selectClauses[0].attributeId = UA_ATTRIBUTEID_VALUE;
    selectClauses[0].browsePath[0] = UA_QUALIFIEDNAME_ALLOC(0, "Message");

    selectClauses[1].typeDefinitionId = UA_NODEID_NUMERIC(0, UA_NS0ID_BASEEVENTTYPE);
    selectClauses[1].browsePathSize = 1;
    selectClauses[1].browsePath = (UA_QualifiedName*)
            UA_Array_new(selectClauses[1].browsePathSize, &UA_TYPES[UA_TYPES_QUALIFIEDNAME]);
    if(!selectClauses[1].browsePath) {
        UA_SimpleAttributeOperand_delete(selectClauses);
        return NULL;
    }
    selectClauses[1].attributeId = UA_ATTRIBUTEID_VALUE;
    selectClauses[1].browsePath[0] = UA_QUALIFIEDNAME_ALLOC(0, "Severity");

    return selectClauses;
}

static bool
opcua_event_collector(UA_Client *client) {
    /* Create a subscription */
    UA_CreateSubscriptionRequest request = UA_CreateSubscriptionRequest_default();
    g_response = UA_Client_Subscriptions_create(client, request, NULL, NULL, NULL);
    if(g_response.responseHeader.serviceResult != UA_STATUSCODE_GOOD) {
        //UA_Client_disconnect(client);
        //UA_Client_delete(client);
        return false;
    }
    UA_UInt32 subId = g_response.subscriptionId;
    UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "Create subscription succeeded, id %u", subId);

    /* Add a MonitoredItem */
    UA_MonitoredItemCreateRequest item;
    UA_MonitoredItemCreateRequest_init(&item);
    item.itemToMonitor.nodeId = UA_NODEID_NUMERIC(0, 2253); // Root->Objects->Server
    item.itemToMonitor.attributeId = UA_ATTRIBUTEID_EVENTNOTIFIER;
    item.monitoringMode = UA_MONITORINGMODE_REPORTING;

    UA_EventFilter_init(&g_filter);
    g_filter.selectClauses = opcua_event_select_clauses();
    g_filter.selectClausesSize = g_nSelectClauses;

    item.requestedParameters.filter.encoding = UA_EXTENSIONOBJECT_DECODED;
    item.requestedParameters.filter.content.decoded.data = &g_filter;
    item.requestedParameters.filter.content.decoded.type = &UA_TYPES[UA_TYPES_EVENTFILTER];

    UA_UInt32 monId = 0;

    g_result = UA_Client_MonitoredItems_createEvent(client, subId,
                                                 UA_TIMESTAMPSTORETURN_BOTH, item,
                                                 &monId, opcua_event_callback, NULL);

    if(g_result.statusCode != UA_STATUSCODE_GOOD) {
        UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                    "Could not add the MonitoredItem");
        return false;
    } else {
        UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                    "Monitoring 'Root->Objects->Server', id %u", g_response.subscriptionId);
    }

    monId = g_result.monitoredItemId;
    return true;
}

static void
opcua_event_delete_callback(UA_Client *client, UA_UInt32 subscriptionId, void *subscriptionContext) {
    UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                "Subscription Id %u was deleted", subscriptionId);
}

#endif /* UA_ENABLE_SUBSCRIPTIONS */

/*
 * Monitor
 */

static void
opcua_monitored_item_callback(UA_Client *client, UA_UInt32 subId, void *subContext,
                           UA_UInt32 monId, void *monContext, UA_DataValue *value) {
    if(!UA_Variant_hasScalarType(&value->value, &UA_TYPES[UA_TYPES_STRING])) {
        UA_LOG_WARNING(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "NCF Ndef");
    }
    UA_String *t_data = (UA_String*) value->value.data;
    char *t_value = (char*) t_data->data;

    JNIEnv *env = nullptr;
    jstring t_string = env->NewStringUTF(t_value);

    std::thread t(jvmCallMessageMethod, t_string);
    t.join();
    //jvmCallMessageMethod(t_string);

/*
    UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND
            , "Read Callback: NFC Ndef - ns=2;i=1016 : message " UA_PRINTF_STRING_FORMAT " "
            , UA_PRINTF_STRING_DATA(*t_data));
*/
}

/*
 * Config Callbacks
 */

static void
opcua_config_state_callback(UA_Client *client, UA_SecureChannelState channelState,
              UA_SessionState sessionState, UA_StatusCode recoveryStatus) {
    switch(channelState) {
        case UA_SECURECHANNELSTATE_CLOSED:
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "The client is disconnected");
            break;
        case UA_SECURECHANNELSTATE_HEL_SENT:
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "Waiting for ack");
            break;
        case UA_SECURECHANNELSTATE_OPN_SENT:
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "Waiting for OPN Response");
            break;
        case UA_SECURECHANNELSTATE_OPEN:
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "A SecureChannel to the server is open");
            break;
        default:
            break;
    }

    switch(sessionState) {
        case UA_SESSIONSTATE_ACTIVATED: {
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "A session with the server is activated");
            /* A new session was created. We need to create the subscription. */
            /* Create a subscription */
            UA_CreateSubscriptionRequest request = UA_CreateSubscriptionRequest_default();
            UA_CreateSubscriptionResponse response = UA_Client_Subscriptions_create(client, request, NULL, NULL, opcua_event_delete_callback);
            if(response.responseHeader.serviceResult == UA_STATUSCODE_GOOD)
                UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                            "Create subscription succeeded, id %u",
                            response.subscriptionId);
            else
                return;

            /* Add a MonitoredItem */
            UA_NodeId messageChanged = UA_NODEID_NUMERIC(3, 1016); /* Ndef message */
            UA_MonitoredItemCreateRequest monRequest = UA_MonitoredItemCreateRequest_default(messageChanged);

            UA_MonitoredItemCreateResult monResponse = UA_Client_MonitoredItems_createDataChange(client, response.subscriptionId,
                                                              UA_TIMESTAMPSTORETURN_BOTH, monRequest,
                                                              NULL, opcua_monitored_item_callback, NULL);
            if(monResponse.statusCode == UA_STATUSCODE_GOOD)
                UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND,
                            "Monitoring NFC Ndef message', id %u",
                            monResponse.monitoredItemId);
        }
            break;
        case UA_SESSIONSTATE_CLOSED:
            UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "Session disconnected");
            break;
        default:
            break;
    }
}

static void
opcua_config_subscription_inactivity_callback (UA_Client *client, UA_UInt32 subId, void *subContext) {
    UA_LOG_INFO(UA_Log_Stdout, UA_LOGCATEGORY_USERLAND, "Inactivity for subscription %u", subId);
}

/*
 * Utility
 */

static std::string
opcua_read_ndef_message() {
    if (!g_connected) {
        return "";
    }

    UA_StatusCode t_retval = UA_STATUSCODE_GOOD;

    /* Read the value attribute of the node. UA_Client_readValueAttribute is a
     * wrapper for the raw read service available as UA_Client_Service_read. */
    UA_Variant t_value; /* Variants can hold scalar values and arrays of any type */
    UA_Variant_init(&t_value);

    /* NodeId of the variable holding the current message */
    const UA_NodeId t_node = UA_NODEID_NUMERIC(3, 1016);
    t_retval = UA_Client_readValueAttribute(g_client, t_node, &t_value);

    if(t_retval == UA_STATUSCODE_GOOD && UA_Variant_hasScalarType(&t_value, &UA_TYPES[UA_TYPES_STRING])) {
        UA_String *t_date = (UA_String *) t_value.data;
        return std::string(reinterpret_cast<char*>(t_date->data), t_date->length);
    }

    /* Clean up */
    UA_Variant_clear(&t_value);
    return "";
}

/*
 * Client
 */

static void
opcua_client_connect() {
    g_client = UA_Client_new();
    UA_ClientConfig *config = UA_Client_getConfig(g_client);
    UA_ClientConfig_setDefault(config);

    /* Set stateCallback */
    //config->stateCallback = opcua_config_state_callback;
    //config->subscriptionInactivityCallback = opcua_config_subscription_inactivity_callback;

    /* default timeout is 5 seconds. Set it to 1 second here for demo */
    config->timeout = 1000;

    UA_StatusCode retval = UA_STATUSCODE_GOOD;
    g_running = true;
    while(g_running) {
        /* if already connected, this will return GOOD and do nothing */
        /* if the connection is closed/errored, the connection will be reset and then reconnected */
        /* Alternatively you can also use UA_Client_getState to get the current state */
        retval = UA_Client_connect(g_client, "opc.tcp://192.168.0.3:4840");
        if (retval != UA_STATUSCODE_GOOD) {
            UA_LOG_ERROR(UA_Log_Stdout, UA_LOGCATEGORY_CLIENT,
                         "Not connected. Retrying to connect in 1 second");

            if (g_connected) {
                g_connected = false;
                std::thread t(jvmCallMethod, g_connected);
                t.join();
                //jvmCallMethod(g_connected);
            }

            /* The connect may timeout after 1 second (see above) or it may fail immediately on network errors */
            /* E.g. name resolution errors or unreachable network. Thus there should be a small sleep here */
            UA_sleep_ms(1000);
        }

        if (!g_connected && (retval == UA_STATUSCODE_GOOD)) {
            g_connected = true;
            std::thread t(jvmCallMethod, g_connected);
            t.join();
            //jvmCallMethod(g_connected);
        }

        if (!g_event) {
#ifdef UA_ENABLE_SUBSCRIPTIONS
            g_event = opcua_event_collector(g_client);
#endif
        }
    }
}

static void
opcua_client_cleanup() {
    g_running = false;
    if (g_thread.joinable()) {
        g_thread.join();
    }

#ifdef UA_ENABLE_SUBSCRIPTIONS
    if (g_event) {
        UA_MonitoredItemCreateResult_clear(&g_result);
        UA_Client_Subscriptions_deleteSingle(g_client, g_response.subscriptionId);
        UA_Array_delete(g_filter.selectClauses, g_nSelectClauses, &UA_TYPES[UA_TYPES_SIMPLEATTRIBUTEOPERAND]);
    }
    g_event = false;
#endif /* UA_ENABLE_SUBSCRIPTIONS */
    /* Disconnects the client internally. */
    if (g_client) {
        UA_Client_delete(g_client);
        g_client = NULL;
    }
    g_connected = false;
}

static UA_StatusCode
opcua_client_up(const int value = 0) {
    if (!g_connected) {
        return UA_STATUSCODE_BADSERVERNOTCONNECTED;
    }

    UA_StatusCode retval = UA_STATUSCODE_GOOD;

    UA_UInt32 t_line = 21;
    UA_UInt32 t_level = 1;
    UA_UInt32 t_id = value;
    UA_Int32 t_error = 0;
    UA_Int32 t_state = 0;

    size_t inputSize = 3;
    size_t outputSize = 2;
    UA_Variant *input = new UA_Variant[inputSize];
    UA_Variant *output = new UA_Variant[outputSize];

    UA_Variant_setScalarCopy(&input[0],&t_line,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[1],&t_level,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[2],&t_id,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&output[0],&t_error,&UA_TYPES[UA_TYPES_INT32]);
    UA_Variant_setScalarCopy(&output[1],&t_state,&UA_TYPES[UA_TYPES_INT32]);

    retval = UA_Client_call(g_client,
                            UA_NODEID_NUMERIC(3, 1022),
                            UA_NODEID_NUMERIC(3, 1023),
                            inputSize, input, &outputSize, &output);

    if (retval == UA_STATUSCODE_GOOD) {
        UA_Int32 val = *(UA_Int32*) output[0].data;
        retval = val;
    }

    /* Clean up */
    UA_Variant_delete(input);
    UA_Variant_delete(output);

    return  retval;
}

static UA_StatusCode
opcua_client_down(const int value = 0) {
    if (!g_connected) {
        return UA_STATUSCODE_BADSERVERNOTCONNECTED;
    }

    UA_StatusCode retval = UA_STATUSCODE_GOOD;

    UA_UInt32 t_line = 20;
    UA_UInt32 t_level = 1;
    UA_UInt32 t_id = value;
    UA_Int32 t_error = 0;
    UA_Int32 t_state = 0;

    size_t inputSize = 3;
    size_t outputSize = 2;
    UA_Variant *input = new UA_Variant[inputSize];
    UA_Variant *output = new UA_Variant[outputSize];

    UA_Variant_setScalarCopy(&input[0],&t_line,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[1],&t_level,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[2],&t_id,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&output[0],&t_error,&UA_TYPES[UA_TYPES_INT32]);
    UA_Variant_setScalarCopy(&output[1],&t_state,&UA_TYPES[UA_TYPES_INT32]);

    retval = UA_Client_call(g_client,
                            UA_NODEID_NUMERIC(3, 1022),
                            UA_NODEID_NUMERIC(3, 1023),
                            inputSize, input, &outputSize, &output);

    if (retval == UA_STATUSCODE_GOOD) {
        UA_Int32 val = *(UA_Int32*) output[0].data;
        retval = val;
    }

    /* Clean up */
    UA_Variant_delete(input);
    UA_Variant_delete(output);

    return  retval;
}

static UA_StatusCode
opcua_client_idle(const int value = 0) {
    if (!g_connected) {
        return UA_STATUSCODE_BADSERVERNOTCONNECTED;
    }

    UA_StatusCode retval = UA_STATUSCODE_GOOD;

    /* Set idle call for up */
    UA_UInt32 t_line = 21;
    UA_UInt32 t_level = 0;
    UA_UInt32 t_id = value;
    UA_Int32 t_error = 0;
    UA_Int32 t_state = 0;

    size_t inputSize = 3;
    size_t outputSize = 2;
    UA_Variant *input = new UA_Variant[inputSize];
    UA_Variant *output = new UA_Variant[outputSize];

    UA_Variant_setScalarCopy(&input[0],&t_line,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[1],&t_level,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[2],&t_id,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&output[0],&t_error,&UA_TYPES[UA_TYPES_INT32]);
    UA_Variant_setScalarCopy(&output[1],&t_state,&UA_TYPES[UA_TYPES_INT32]);

    retval = UA_Client_call(g_client,
                            UA_NODEID_NUMERIC(3, 1022),
                            UA_NODEID_NUMERIC(3, 1023),
                            inputSize, input, &outputSize, &output);

    if (retval == UA_STATUSCODE_GOOD) {
        UA_Int32 val = *(UA_Int32*) output[0].data;
        retval = val;
    }

    /* Set idle call for down */
    t_line = 20;
    t_level = 0;
    t_id = value;
    t_error = 0;
    t_state = 0;

    UA_Variant_init(input);
    UA_Variant_init(output);

    UA_Variant_setScalarCopy(&input[0],&t_line,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[1],&t_level,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&input[2],&t_id,&UA_TYPES[UA_TYPES_UINT32]);
    UA_Variant_setScalarCopy(&output[0],&t_error,&UA_TYPES[UA_TYPES_INT32]);
    UA_Variant_setScalarCopy(&output[1],&t_state,&UA_TYPES[UA_TYPES_INT32]);

    retval = UA_Client_call(g_client,
                            UA_NODEID_NUMERIC(3, 1022),
                            UA_NODEID_NUMERIC(3, 1023),
                            inputSize, input, &outputSize, &output);

    if (retval == UA_STATUSCODE_GOOD) {
        UA_Int32 val = *(UA_Int32*) output[0].data;
        retval = val;
    }

    /* Clean up */
    UA_Variant_delete(input);
    UA_Variant_delete(output);

    return  retval;
}

/** ControlActivity
 * See the corresponding Java source file located at:
 * app/src/main/java/com/sentenz/ControlActivity.java
 */

extern "C"
JNIEXPORT void JNICALL
Java_com_sentenz_controlz_ControlActivity_jniOpcUaConnect( JNIEnv * env, jobject thiz ) {
    /* Java method call */
    env->GetJavaVM(&g_jvm);
    g_obj = env->NewGlobalRef(thiz);

    /* CPP thread */
    g_thread = std::thread(opcua_client_connect);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sentenz_controlz_ControlActivity_jniOpcUaCleanup( JNIEnv * env, jobject thiz ) {
    opcua_client_cleanup();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sentenz_controlz_ControlActivity_jniOpcUaTaskUp( JNIEnv * env, jobject thiz ) {
    UA_StatusCode retval = opcua_client_up();

    /* JVM method call */
/*
    jclass clazz = env->GetObjectClass(thiz);
    jmethodID jniConnectCallback = env->GetMethodID(clazz, "jniConnectCallback", "()V");
    env->CallVoidMethod(thiz, jniConnectCallback);
*/

    return retval;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sentenz_controlz_ControlActivity_jniOpcUaTaskDown( JNIEnv * env, jobject thiz ) {
    UA_StatusCode retval = opcua_client_down();
    return retval;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sentenz_controlz_ControlActivity_jniOpcUaTaskIdle( JNIEnv * env, jobject thiz ) {
    UA_StatusCode retval = opcua_client_idle();
    return retval;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sentenz_controlz_ControlActivity_jniOpcUaMessage(JNIEnv *env, jobject thiz) {
    //std::string t_string = opcua_read_ndef_message();

    auto future = std::async(std::launch::async, opcua_read_ndef_message);
    std::string t_string = future.get();

    return env->NewStringUTF(t_string.c_str());
}

/** PaternosterActivity
 * See the corresponding Java source file located at:
 * app/src/main/java/com/sentenz/PaternosterActivity.java
 */

extern "C"
JNIEXPORT void JNICALL
Java_com_sentenz_controlz_PaternosterActivity_jniOpcUaConnect(JNIEnv *env, jobject thiz) {
    /* Java method call */
    env->GetJavaVM(&g_jvm);
    g_obj = env->NewGlobalRef(thiz);

    /* CPP thread */
    g_thread = std::thread(opcua_client_connect);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_sentenz_controlz_PaternosterActivity_jniOpcUaCleanup(JNIEnv *env, jobject thiz) {
    opcua_client_cleanup();
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sentenz_controlz_PaternosterActivity_jniOpcUaTaskUp(JNIEnv *env, jobject thiz, jint value) {
    UA_StatusCode retval = opcua_client_up((int) value);
    return retval;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sentenz_controlz_PaternosterActivity_jniOpcUaTaskDown(JNIEnv *env, jobject thiz, jint value) {
    UA_StatusCode retval = opcua_client_down((int) value);
    return retval;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_sentenz_controlz_PaternosterActivity_jniOpcUaTaskIdle(JNIEnv *env, jobject thiz, jint value) {
    UA_StatusCode retval = opcua_client_idle((int) value);
    return retval;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_sentenz_controlz_PaternosterActivity_jniOpcUaMessage(JNIEnv *env, jobject thiz) {
    //std::string t_string = opcua_read_ndef_message();

    auto future = std::async(std::launch::async, opcua_read_ndef_message);
    std::string t_string = future.get();

    return env->NewStringUTF(t_string.c_str());
}
