use std::fs::File;
use std::io::Read;
use std::os::unix::io::FromRawFd;
use std::path::Path;
use tokenizers::Tokenizer;

use crate::engine::generate_with_safetensors;

/// Real SafeTensors inference engine from Android ParcelFileDescriptor (FD)
pub fn run_candle_safetensors_from_fd(
    weights_fd: i32,
    tokenizer_json: &str,
    config_json: &str,
    prompt: &str,
    temperature: f64,
    top_p: f64,
    max_tokens: usize,
) -> Result<String, String> {
    if weights_fd < 0 {
        return Err("File descriptor de SafeTensors inválido (fd < 0).".to_string());
    }

    if tokenizer_json.trim().is_empty() {
        return Err("Contenido de tokenizer.json vacío o no accesible.".to_string());
    }

    // 1. Cargar tokenizador directamente desde bytes JSON
    let tokenizer = Tokenizer::from_bytes(tokenizer_json.as_bytes())
        .map_err(|e| format!("Error cargando tokenizador desde JSON: {}", e))?;

    // 2. Mapeo en memoria mmap directamente desde el File Descriptor de Android
    let file = unsafe { File::from_raw_fd(weights_fd) };
    let mmap = unsafe { memmap2::Mmap::map(&file) }
        .map_err(|e| format!("Error mapeando memoria mmap para tensores (fd {}): {}", weights_fd, e))?;

    // Evitar que Rust cierre el descriptor de archivo de Android al salir de ámbito
    std::mem::forget(file);

    let safetensors_data = safetensors::SafeTensors::deserialize(&mmap)
        .map_err(|e| format!("Error deserializando SafeTensors desde mmap: {}", e))?;

    generate_with_safetensors(
        &safetensors_data,
        &tokenizer,
        config_json,
        prompt,
        temperature,
        top_p,
        max_tokens,
    )
}

/// Real SafeTensors inference engine from file path
pub fn run_candle_safetensors_inference(
    weights_path: &str,
    tokenizer_path: &str,
    config_path: &str,
    prompt: &str,
    temperature: f64,
    top_p: f64,
    max_tokens: usize,
) -> Result<String, String> {
    if !Path::new(weights_path).exists() {
        return Err(format!("Archivo de tensores no encontrado en ruta: {}", weights_path));
    }
    if !Path::new(tokenizer_path).exists() {
        return Err(format!("Archivo de tokenizador no encontrado en ruta: {}", tokenizer_path));
    }

    let tokenizer = Tokenizer::from_file(tokenizer_path)
        .map_err(|e| format!("Error cargando tokenizador (tokenizer.json): {}", e))?;

    let mut config_json = String::new();
    if Path::new(config_path).exists() {
        if let Ok(mut f) = File::open(config_path) {
            let _ = f.read_to_string(&mut config_json);
        }
    }

    let file = File::open(weights_path)
        .map_err(|e| format!("Error abriendo archivo SafeTensors: {}", e))?;
    let mmap = unsafe { memmap2::Mmap::map(&file) }
        .map_err(|e| format!("Error mapeando memoria mmap para tensores: {}", e))?;

    let safetensors_data = safetensors::SafeTensors::deserialize(&mmap)
        .map_err(|e| format!("Error deserializando SafeTensors: {}", e))?;

    generate_with_safetensors(
        &safetensors_data,
        &tokenizer,
        &config_json,
        prompt,
        temperature,
        top_p,
        max_tokens,
    )
}
