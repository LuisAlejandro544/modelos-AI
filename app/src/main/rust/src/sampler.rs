use std::sync::atomic::{AtomicBool, Ordering};

pub static INTERRUPT_FLAG: AtomicBool = AtomicBool::new(false);

pub fn request_cancellation() {
    INTERRUPT_FLAG.store(true, Ordering::SeqCst);
}

pub fn reset_cancellation() {
    INTERRUPT_FLAG.store(false, Ordering::SeqCst);
}

pub fn is_cancelled() -> bool {
    INTERRUPT_FLAG.load(Ordering::Relaxed)
}
