package com.flowshift.editor.webview;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

/**
 * WebViewInjector v8.9.3
 * Kapselt die JavaScript-Logik für die souveräne Inspektion und Interaktion.
 * Isoliert den Controller von JS-Code und sorgt für saubere Injektion.
 */
public class WebViewInjector {
    
    // Das vollständige, modulare JavaScript (CSS + Hover + Klick)
    // Wir nutzen Java 23 Text Blocks für maximale Lesbarkeit.
    private static final String INSPECTOR_SCRIPT =  """
    	    (function() {
            var style = document.createElement('style');
            style.innerHTML = '.fs-hover { outline: 2px solid #20BFDF !important; cursor: crosshair !important; }';
            document.head.appendChild(style);

            document.onmouseover = function(e) {
                var target = e.target.closest('[data-uid]');
                if (window.last) window.last.classList.remove('fs-hover');
                if (target) { target.classList.add('fs-hover'); window.last = target; }
            };

            document.onclick = function(e) {
                try {
                    var target = e.target.closest('[data-uid]') || e.target;
                    var uid = target.getAttribute('data-uid') || '-1';
                    var tagName = target.tagName.toLowerCase();
                    
                    var selectors = [];
                    var curr = target;
                    while(curr && curr.tagName !== 'HTML') {
                        selectors.push(curr.tagName.toLowerCase());
                        if(curr.id) selectors.push('#' + curr.id);
                        if(curr.className) {
                            curr.className.split(/\\s+/).forEach(c => {
                                if(c && c !== 'fs-hover') selectors.push('.' + c);
                            });
                        }
                        curr = curr.parentElement;
                    }

                    var text = target.innerText ? target.innerText.substring(0, 40).replace(/[\\u00A0\\n\\r]+/g, ' ').trim() : '';
                    var sig = (target.outerHTML.match(/^<[^>]+>/) || [''])[0];

                    var occ = 0;
                    var all = document.getElementsByTagName(target.tagName);
                    for(var i=0; i<all.length; i++) {
                        if(all[i] === target) break;
                        var s = (all[i].outerHTML.match(/^<[^>]+>/) || [''])[0];
                        if(s === sig) occ++;
                    }

                    javaBridge.onElementClicked(tagName, text, selectors.join(','), sig, uid, occ);
                } catch(err) { console.error(err); }
                e.preventDefault(); e.stopPropagation();
            };
        })();
        """;

    /**
     * Registriert die Java-Brücke und injiziert das Inspektor-Skript.
     * Muss aufgerufen werden, wenn der LoadWorker den Status SUCCEEDED erreicht.
     * 
     * @param engine Die WebEngine der Vorschau.
     * @param bridgeObject Das Java-Objekt (SovereignBridge), das aufgerufen werden soll.
     */
    public static void inject(WebEngine engine, Object bridgeObject) {
        if (engine.getLoadWorker().getState() != Worker.State.SUCCEEDED) return;
        
        try {
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaBridge", bridgeObject);
            engine.executeScript(INSPECTOR_SCRIPT);
        } catch (Exception e) {
            System.err.println("Failed to inject Sovereign Inspector: " + e.getMessage());
        }
    }
}