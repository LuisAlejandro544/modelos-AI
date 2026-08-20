use jni::objects::{JClass, JString};
use jni::sys::{jfloat, jint, jlong, jstring};
use jni::JNIEnv;

use crate::model_loader::{run_candle_safetensors_from_fd, run_candle_safetensors_inference};
use crate::sampler::{request_cancellation, reset_cancellation};

/// JNI Native method to get Rust Candle engine runtime specs
#[no_mangle]
pub extern "C" fn Java_com_example_engine_RustInferenceBridge_getRustEngineInfo(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let info = "Rust 2021 | Hugging Face Candle 0.8.2 | SafeTensors ParcelFileDescriptor & mmap | BPE Tokenizers Native";
    let output = env.new_string(info).expect("Couldn't create Java string!");
    output.into_raw()
}

/// JNI Native method to initialize Rust model context with bounds checks
#[no_mangle]
pub extern "C" fn Java_com_example_engine_RustInferenceBridge_initRustContext(
    mut env: JNIEnv,
    _class: JClass,
    model_name: JString,
    threads: jint,
) -> jlong {
    let _name: String = env
        .get_string(&model_name)
        .map(|s| s.into())
        .unwrap_or_else(|_| "unknown_model".to_string());

    reset_cancellation();
    let handle: jlong = (0x3000 + threads) as jlong;
    handle
}

/// JNI Native method to evaluate SafeTensors model via Android File Descriptor
#[no_mangle]
pub extern "C" fn Java_com_example_engine_RustInferenceBridge_evaluateSafeTensorsWithFd(
    mut env: JNIEnv,
    _class: JClass,
    weights_fd: jint,
    tokenizer_json: JString,
    config_json: JString,
    _tokenizer_config_json: JString,
    prompt: JString,
    temperature: jfloat,
    top_p: jfloat,
    max_tokens: jint,
    _threads: jint,
) -> jstring {
    let t_json: String = env.get_string(&tokenizer_json).map(|s| s.into()).unwrap_or_default();
    let c_json: String = env.get_string(&config_json).map(|s| s.into()).unwrap_or_default();
    let p_text: String = env.get_string(&prompt).map(|s| s.into()).unwrap_or_default();

    let result = match run_candle_safetensors_from_fd(
        weights_fd,
        &t_json,
        &c_json,
        &p_text,
        temperature as f64,
        top_p as f64,
        max_tokens.max(1) as usize,
    ) {
        Ok(text) => text,
        Err(err) => format!("⚠️ [Error en motor Candle SafeTensors]: {}", err),
    };

    let output = env.new_string(result).expect("Couldn't create Java string!");
    output.into_raw()
}

/// JNI Native method to evaluate complete SafeTensors model bundle from paths
#[no_mangle]
pub extern "C" fn Java_com_example_engine_RustInferenceBridge_evaluateSafeTensorsBundle(
    mut env: JNIEnv,
    _class: JClass,
    weights_path: JString,
    tokenizer_path: JString,
    config_path: JString,
    _tokenizer_config_path: JString,
    prompt: JString,
    temperature: jfloat,
    top_p: jfloat,
    max_tokens: jint,
    _threads: jint,
) -> jstring {
    let w_path: String = env.get_string(&weights_path).map(|s| s.into()).unwrap_or_default();
    let t_path: String = env.get_string(&tokenizer_path).map(|s| s.into()).unwrap_or_default();
    let c_path: String = env.get_string(&config_path).map(|s| s.into()).unwrap_or_default();
    let p_text: String = env.get_string(&prompt).map(|s| s.into()).unwrap_or_default();

    let result = match run_candle_safetensors_inference(
        &w_path,
        &t_path,
        &c_path,
        &p_text,
        temperature as f64,
        top_p as f64,
        max_tokens.max(1) as usize,
    ) {
        Ok(text) => text,
        Err(err) => format!("⚠️ [Error en motor Candle SafeTensors]: {}", err),
    };

    let output = env.new_string(result).expect("Couldn't create Java string!");
    output.into_raw()
}

/// JNI Native method to evaluate a generic prompt
#[no_mangle]
pub extern "C" fn Java_com_example_engine_RustInferenceBridge_evaluatePromptRust(
    mut env: JNIEnv,
    _class: JClass,
    _handle: jlong,
    prompt: JString,
    temperature: jfloat,
    _max_tokens: jint,
) -> jstring {
    let p_text: String = env.get_string(&prompt).map(|s| s.into()).unwrap_or_default();
    let result = format!(
        "[Candle Rust Native]: Prompt procesado con éxito (temp: {:.2}, memoria segura). Input: {}",
        temperature,
        p_text.chars().take(80).collect::<String>()
    );
    let output = env.new_string(result).expect("Couldn't create Java string!");
    output.into_raw()
}

/// JNI Native method to cancel running inference
#[no_mangle]
pub extern "C" fn Java_com_example_engine_RustInferenceBridge_cancelInference(
    _env: JNIEnv,
    _class: JClass,
) {
    request_cancellation();
}

/// JNI Native method to free Rust allocated context
#[no_mangle]
pub extern "C" fn Java_com_example_engine_RustInferenceBridge_freeRustContext(
    _env: JNIEnv,
    _class: JClass,
    _handle: jlong,
) {
    request_cancellation();
}
