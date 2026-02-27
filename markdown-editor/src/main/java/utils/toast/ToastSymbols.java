package utils.toast;

/**
 * Central symbol management for toast notifications.
 * Allows easy switching between different icon fonts/sets.
 */
public final class ToastSymbols {
    
    // Aktuelles Symbol-Set (kann zur Laufzeit gewechselt werden)
    private static SymbolSet currentSet = SymbolSet.UNICODE;
    
    // ===== SYMBOL DEFINITIONS =====
    public enum SymbolSet {
        UNICODE {
            public String pause() { return "⏸"; }
            public String close() { return "✕"; }
            public String info() { return "ℹ"; }
            public String warning() { return "⚠"; }
            public String success() { return "✓"; }
            public String error() { return "✗"; }
        },
        FONTAWESOME {
            public String pause() { return "\uf04c"; }  // fa-pause
            public String close() { return "\uf00d"; }  // fa-times
            public String info() { return "\uf129"; }   // fa-info
            public String warning() { return "\uf071"; } // fa-exclamation-triangle
            public String success() { return "\uf00c"; } // fa-check
            public String error() { return "\uf06a"; }   // fa-exclamation-circle
        };
        
        public abstract String pause();
        public abstract String close();
        public abstract String info();
        public abstract String warning();
        public abstract String success();
        public abstract String error();
    }
    
    public static void setSymbolSet(SymbolSet set) {
        currentSet = set;
    }
    
    public static String pause() { return currentSet.pause(); }
    public static String close() { return currentSet.close(); }
    public static String info() { return currentSet.info(); }
    public static String warning() { return currentSet.warning(); }
    public static String success() { return currentSet.success(); }
    public static String error() { return currentSet.error(); }
    
    private ToastSymbols() {}
}