(function () {
    // =============================================
    // 1. SCROLL NAVIGATION HIGHLIGHTING
    // =============================================
    function updateActiveNavLink() {
        const sections = document.querySelectorAll('h2[id], h3[id]');
        const navLinks = document.querySelectorAll('.floating-nav a');
        let current = "";

        sections.forEach(section => {
            const sectionTop = section.offsetTop;
            if (window.pageYOffset >= sectionTop - 200) {
                current = section.getAttribute('id');
            }
        });

        navLinks.forEach(link => {
            link.classList.remove('active');
            if (link.getAttribute('href') === `#${current}`) {
                link.classList.add('active');
            }
        });
    }

    window.addEventListener('scroll', updateActiveNavLink);
    window.addEventListener('load', updateActiveNavLink);

    // Smooth scroll für Nav-Links
    document.querySelectorAll('.floating-nav a').forEach(link => {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('href').substring(1);
            const targetElement = document.getElementById(targetId);
            if (targetElement) {
                targetElement.scrollIntoView({ behavior: 'smooth' });
            }
        });
    });

    // =============================================
    // 2. CUSTOM CURSOR FÜR BILDER
    // =============================================
    (function () {
        document.addEventListener('DOMContentLoaded', function () {
            const cursor = document.getElementById('customCursor');
            const cards = document.querySelectorAll('.image-card');

            // Wenn es keinen Cursor oder keine Cards gibt, abbrechen
            if (!cursor || cards.length === 0) return;

            let lastMouseX = 0, lastMouseY = 0;
            let isCursorActive = false;

            // Mausposition speichern (Viewport-Koordinaten)
            document.addEventListener('mousemove', (e) => {
                lastMouseX = e.clientX;
                lastMouseY = e.clientY;
                if (isCursorActive) {
                    cursor.style.left = lastMouseX + 'px';
                    cursor.style.top = lastMouseY + 'px';
                }
            });

            // Prüfen, ob Maus über einer Card ist
            function isMouseOverCard() {
                for (let card of cards) {
                    const rect = card.getBoundingClientRect();
                    if (lastMouseX >= rect.left && lastMouseX <= rect.right &&
                        lastMouseY >= rect.top && lastMouseY <= rect.bottom) {
                        return true;
                    }
                }
                return false;
            }

            function updateCursorVisibility() {
                const overCard = isMouseOverCard();

                if (overCard) {
                    cursor.style.display = 'block';
                    cursor.style.left = lastMouseX + 'px';
                    cursor.style.top = lastMouseY + 'px';
                    isCursorActive = true;
                    document.body.style.cursor = 'none';
                } else {
                    cursor.style.display = 'none';
                    isCursorActive = false;
                    document.body.style.cursor = 'default';
                }
            }

            // Events für Cards
            cards.forEach(card => {
                card.addEventListener('mouseenter', updateCursorVisibility);
                card.addEventListener('mouseleave', () => {
                    cursor.style.display = 'none';
                    isCursorActive = false;
                    document.body.style.cursor = 'default';
                });
            });

            // Bei Mausbewegung immer prüfen
            document.addEventListener('mousemove', (e) => {
                lastMouseX = e.clientX;
                lastMouseY = e.clientY;
                updateCursorVisibility();
            });

            // Beim Scrollen neu prüfen
            window.addEventListener('scroll', updateCursorVisibility);
        });
    })();

    // =============================================
    // 3. WONDERSHARE-STYLE BILDZOOM MIT NAVIGATION
    // =============================================

    // CSS für Lightbox mit perfektem X und Next/Prev Buttons
    const style = document.createElement('style');
    style.textContent = `
        .ws-lightbox {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.98);
            z-index: 99999;
            display: flex;
            justify-content: center;
            align-items: center;
            backdrop-filter: blur(15px);
            -webkit-backdrop-filter: blur(15px);
            animation: wsFadeIn 0.25s ease-out;
        }
        
        .ws-lightbox-content {
            position: relative;
            display: flex;
            justify-content: center;
            align-items: center;
            width: 100%;
            height: 100%;
        }
        
        .ws-lightbox img {
            max-width: 80vw;
            max-height: 80vh;
            border: 4px solid #00d1ff;
            border-radius: 20px;
            box-shadow: 0 25px 60px rgba(0, 209, 255, 0.4);
            transform: scale(0.95);
            animation: wsZoomIn 0.3s cubic-bezier(0.2, 0.9, 0.3, 1.1) forwards;
        }
        
        .ws-close {
            position: absolute;
            top: 30px;
            right: 40px;
            width: 50px;
            height: 50px;
            background: rgba(0, 209, 255, 0.15);
            border: 2px solid #00d1ff;
            border-radius: 50%;
            cursor: pointer;
            display: flex;
            justify-content: center;
            align-items: center;
            transition: all 0.2s ease;
            z-index: 100000;
            box-shadow: 0 0 20px rgba(0, 209, 255, 0.3);
        }
        
        .ws-close:hover {
            background: #00d1ff;
            transform: scale(1.1);
            box-shadow: 0 0 30px rgba(0, 209, 255, 0.6);
        }
        
        .ws-close::before,
        .ws-close::after {
            content: '';
            position: absolute;
            width: 25px;
            height: 3px;
            background-color: white;
            border-radius: 3px;
        }
        
        .ws-close::before {
            transform: rotate(45deg);
        }
        
        .ws-close::after {
            transform: rotate(-45deg);
        }
        
        .ws-close:hover::before,
        .ws-close:hover::after {
            background-color: #0b0f19;
        }
        
        .ws-nav {
            position: absolute;
            top: 50%;
            transform: translateY(-50%);
            width: 60px;
            height: 60px;
            background: rgba(0, 209, 255, 0.15);
            border: 2px solid #00d1ff;
            border-radius: 50%;
            cursor: pointer;
            display: flex;
            justify-content: center;
            align-items: center;
            transition: all 0.2s ease;
            z-index: 100000;
            box-shadow: 0 0 20px rgba(0, 209, 255, 0.3);
        }
        
        .ws-nav:hover {
            background: #00d1ff;
            transform: translateY(-50%) scale(1.1);
            box-shadow: 0 0 30px rgba(0, 209, 255, 0.6);
        }
        
        .ws-nav.prev {
            left: 40px;
        }
        
        .ws-nav.next {
            right: 40px;
        }
        
        .ws-nav::before {
            content: '';
            width: 15px;
            height: 15px;
            border-left: 3px solid white;
            border-bottom: 3px solid white;
            transform: rotate(45deg);
            margin-left: 5px;
        }
        
        .ws-nav.next::before {
            transform: rotate(-135deg);
            margin-left: -5px;
        }
        
        .ws-nav.prev::before {
            transform: rotate(45deg);
        }
        
        .ws-nav:hover::before {
            border-color: #0b0f19;
        }
        
        .ws-counter {
            position: absolute;
            bottom: 30px;
            left: 50%;
            transform: translateX(-50%);
            color: white;
            background: rgba(0, 209, 255, 0.15);
            padding: 10px 25px;
            border-radius: 40px;
            font-size: 1rem;
            border: 1px solid #00d1ff;
            letter-spacing: 0.1em;
            backdrop-filter: blur(5px);
            box-shadow: 0 0 20px rgba(0, 209, 255, 0.3);
            z-index: 100000;
        }
        
        @keyframes wsFadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        
        @keyframes wsZoomIn {
            from { 
                transform: scale(0.8);
                opacity: 0;
            }
            to { 
                transform: scale(1);
                opacity: 1;
            }
        }
        
        .ws-lightbox-fade-out {
            animation: wsFadeOut 0.2s ease-in forwards !important;
        }
        
        @keyframes wsFadeOut {
            from { opacity: 1; }
            to { opacity: 0; }
        }
        
        .ws-nav.disabled {
            opacity: 0.3;
            cursor: not-allowed;
            pointer-events: none;
            border-color: #475569;
            background: rgba(71, 85, 105, 0.15);
        }
    `;
    document.head.appendChild(style);

    // Alle Bild-Cards finden
    const imageCards = document.querySelectorAll('.image-card');
    const images = [];

    // Alle Bilder sammeln
    imageCards.forEach((card, index) => {
        const img = card.querySelector('img');
        if (img) {
            images.push({
                element: img,
                src: img.src,
                alt: img.alt,
                card: card
            });

            // Click-Event für jedes Bild
            card.addEventListener('click', function (e) {
                e.stopPropagation();
                openLightbox(index);
            });

            card.style.cursor = 'zoom-in';
        }
    });

    // Lightbox öffnen
    function openLightbox(startIndex) {
        // Bestehende Lightbox entfernen
        const existing = document.querySelector('.ws-lightbox');
        if (existing) existing.remove();

        // Lightbox Container
        const lightbox = document.createElement('div');
        lightbox.className = 'ws-lightbox';

        // Content Container
        const content = document.createElement('div');
        content.className = 'ws-lightbox-content';

        // Bild-Element
        const img = document.createElement('img');
        img.src = images[startIndex].src;
        img.alt = images[startIndex].alt;

        // X Close Button
        const closeBtn = document.createElement('div');
        closeBtn.className = 'ws-close';
        closeBtn.setAttribute('aria-label', 'Close');

        // Escape-Hinweis (oben links)
        const escapeHint = document.createElement('div');
        escapeHint.style.cssText = `
    position: absolute;
    top: 40px;
    left: 40px;
    color: #94a3b8;
    font-size: 0.9rem;
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(0, 209, 255, 0.1);
    padding: 8px 16px;
    border-radius: 40px;
    border: 1px solid rgba(0, 209, 255, 0.2);
    z-index: 100000;
    backdrop-filter: blur(5px);
    letter-spacing: 0.3px;
`;

        escapeHint.innerHTML = `
    <span style="font-size: 1.4rem; opacity: 0.9;">⌨</span>
    <span>:</span>
    <span>Close with</span>
    <kbd style="background: rgba(0, 209, 255, 0.15); color: #00d1ff; padding: 4px 8px; border-radius: 4px; font-family: monospace; font-weight: 600; font-size: 0.85rem; border: 1px solid #00d1ff;">ESC</kbd>
    <span style="margin: 0 5px;">|</span>
    <span style="margin-left: 5px;">←</span>
    <span>→</span>
    <span style="margin-left: 5px;">navigate</span>
`;

        content.appendChild(escapeHint);

        // Navigation und Counter (nur wenn mehrere Bilder)
        let prevBtn, nextBtn, counter;
        let currentIndex = startIndex;

        if (images.length > 1) {
            // Prev Button
            prevBtn = document.createElement('div');
            prevBtn.className = 'ws-nav prev';
            prevBtn.setAttribute('aria-label', 'Previous image');

            // Next Button
            nextBtn = document.createElement('div');
            nextBtn.className = 'ws-nav next';
            nextBtn.setAttribute('aria-label', 'Next image');

            // Counter
            counter = document.createElement('div');
            counter.className = 'ws-counter';
            counter.textContent = `${startIndex + 1} / ${images.length}`;

            // Event Listener für Navigation
            prevBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                if (currentIndex > 0) {
                    currentIndex--;
                    updateImage();
                }
            });

            nextBtn.addEventListener('click', function (e) {
                e.stopPropagation();
                if (currentIndex < images.length - 1) {
                    currentIndex++;
                    updateImage();
                }
            });

            // Update Funktion
            function updateImage() {
                img.style.opacity = '0.5';
                setTimeout(() => {
                    img.src = images[currentIndex].src;
                    img.alt = images[currentIndex].alt;
                    counter.textContent = `${currentIndex + 1} / ${images.length}`;

                    // Prev/Next Buttons deaktivieren/aktivieren
                    if (prevBtn) {
                        if (currentIndex === 0) {
                            prevBtn.classList.add('disabled');
                        } else {
                            prevBtn.classList.remove('disabled');
                        }
                    }

                    if (nextBtn) {
                        if (currentIndex === images.length - 1) {
                            nextBtn.classList.add('disabled');
                        } else {
                            nextBtn.classList.remove('disabled');
                        }
                    }

                    img.style.opacity = '1';
                }, 150);
            }

            // Initialen Status der Buttons setzen
            if (currentIndex === 0) {
                prevBtn.classList.add('disabled');
            }
            if (currentIndex === images.length - 1) {
                nextBtn.classList.add('disabled');
            }

            content.appendChild(prevBtn);
            content.appendChild(nextBtn);
            content.appendChild(counter);

            // Tastatur-Navigation
            const keyHandler = function (e) {
                if (e.key === 'Escape') {
                    closeLightbox(lightbox);
                    document.removeEventListener('keydown', keyHandler);
                } else if (e.key === 'ArrowLeft' && currentIndex > 0) {
                    currentIndex--;
                    updateImage();
                } else if (e.key === 'ArrowRight' && currentIndex < images.length - 1) {
                    currentIndex++;
                    updateImage();
                }
            };

            document.addEventListener('keydown', keyHandler);

            // Cleanup Funktion
            lightbox.addEventListener('remove', function () {
                document.removeEventListener('keydown', keyHandler);
            });
        }

        // Content zusammenbauen
        content.appendChild(img);
        content.appendChild(closeBtn);
        lightbox.appendChild(content);

        // Close Funktion
        closeBtn.addEventListener('click', function () {
            closeLightbox(lightbox);
        });

        // Klick auf Hintergrund schließt
        lightbox.addEventListener('click', function (e) {
            if (e.target === lightbox) {
                closeLightbox(lightbox);
            }
        });

        document.body.appendChild(lightbox);
        document.body.style.overflow = 'hidden';
    }

    // Lightbox schließen
    function closeLightbox(lightbox) {
        lightbox.classList.add('ws-lightbox-fade-out');
        setTimeout(() => {
            lightbox.remove();
            document.body.style.overflow = '';
        }, 200);
    }

    // Escape-Taste global
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            const lightbox = document.querySelector('.ws-lightbox');
            if (lightbox) {
                closeLightbox(lightbox);
            }
        }
    });
})();