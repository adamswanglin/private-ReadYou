package me.ash.reader.ui.component.webview

object WebViewScript {

    fun get(boldCharacters: Boolean, isEbookModeEnabled: Boolean = false) = """
const BR_WORD_STEM_PERCENTAGE = 0.7;
const MAX_FIXATION_PARTS = 4;
const FIXATION_LOWER_BOUND = 0
function highlightText(sentenceText) {
	return sentenceText.replace(/\p{L}+/gu, (word) => {
		const { length } = word;

		const brWordStemWidth = length > 3 ? Math.round(length * BR_WORD_STEM_PERCENTAGE) : length;

		const firstHalf = word.slice(0, brWordStemWidth);
		const secondHalf = word.slice(brWordStemWidth);
		var htmlWord = "<br-bold>";
        htmlWord += makeFixations(firstHalf);
        htmlWord += "</br-bold>";
        if (secondHalf.length) {
            htmlWord += "<br-edge>";
            htmlWord += makeFixations(secondHalf);
            htmlWord += "</br-edge>";
        }
		return htmlWord;
	});
}

function makeFixations(textContent) {
	const COMPUTED_MAX_FIXATION_PARTS = textContent.length >= MAX_FIXATION_PARTS ? MAX_FIXATION_PARTS : textContent.length;

	const fixationWidth = Math.ceil(textContent.length * (1 / COMPUTED_MAX_FIXATION_PARTS));

	if (fixationWidth === FIXATION_LOWER_BOUND) {
		return '<br-fixation fixation-strength="1">' + textContent + '</br-fixation>';
	}

	const fixationsSplits = new Array(COMPUTED_MAX_FIXATION_PARTS).fill(null).map((item, index) => {
		const wordStartBoundary = index * fixationWidth;
		const wordEndBoundary = wordStartBoundary + fixationWidth > textContent.length ? textContent.length : wordStartBoundary + fixationWidth;

		return `<br-fixation fixation-strength="` + (index + 1) + `">` + textContent.slice(wordStartBoundary, wordEndBoundary) + `</br-fixation>`;
	});

	return fixationsSplits.join('');
}

const IGNORE_NODE_TAGS = ['STYLE', 'SCRIPT', 'BR-SPAN', 'BR-FIXATION', 'BR-BOLD', 'BR-EDGE', 'SVG', 'INPUT', 'TEXTAREA'];
function parseNode(node) {
    if (!node?.parentElement?.tagName || IGNORE_NODE_TAGS.includes(node.parentElement.tagName)) {
        return;
    }
    
    if (node.nodeType === Node.TEXT_NODE && node.nodeValue.length) {
        try {
            const brSpan = document.createElement('br-span');
            brSpan.innerHTML = highlightText(node.nodeValue);
            if (brSpan.childElementCount === 0) return;
            node.parentElement.replaceChild(brSpan, node); // JiffyReader keeps the old element around, but we don't need it
        } catch (e) {
            console.error('Error parsing text node:', e);
        }
        return;
    }
    
    if (node.hasChildNodes()) [...node.childNodes].forEach(parseNode);
}

function setBold(enabled) {
    if (enabled) {
        document.body.setAttribute("br-mode", "on");
        [...document.body.childNodes].forEach(parseNode);
    } else {
        document.body.setAttribute("br-mode", "off");
    }
}

${if (boldCharacters) "setBold(true);" else ""}

var images = document.querySelectorAll("img");

images.forEach(function(img) {
    img.onload = function() {
        img.classList.add("loaded");
        console.log("Image width:", img.width, "px");
        if (img.width < 412) {
            img.classList.add("thin");
        }
    };

    img.onerror = function() {
        console.error("Failed to load image:", img.src);
    };
});

${if (isEbookModeEnabled) """
// 电子书模式：左右点击翻页
var lastClickTime = 0;
var clickThrottle = 300; // 300ms防抖

// 监听整个文档的点击事件，包括空白区域
document.addEventListener('click', function(event) {
    var currentTime = Date.now();
    if (currentTime - lastClickTime < clickThrottle) {
        return; // 忽略过于频繁的点击
    }
    // 检查是否点击的是链接或图片
    var target = event.target;
    var isLink = target.tagName === 'A' || target.closest('a');
    var isImage = target.tagName === 'IMG';
    
    if (isLink || isImage) {
        return; // 让链接和图片的默认行为执行
    }
    
    // 获取点击位置
    var clickX = event.clientX;
    var screenWidth = window.innerWidth;
    var leftThird = screenWidth * 0.33;
    var rightThird = screenWidth * 0.67;
    
    if (clickX < leftThird) {
        // 点击左侧区域，向上翻页
        lastClickTime = currentTime;
        if (typeof JavaScriptInterface !== 'undefined') {
            JavaScriptInterface.onPageUp();
        }
        event.preventDefault();
        event.stopPropagation();
    } else if (clickX > rightThird) {
        // 点击右侧区域，向下翻页
        lastClickTime = currentTime;
        if (typeof JavaScriptInterface !== 'undefined') {
            JavaScriptInterface.onPageDown();
        }
        event.preventDefault();
        event.stopPropagation();
    }
    // 中间区域不做处理，保持默认行为
}, true);

// 确保body元素有足够的高度来接收点击事件
document.body.style.minHeight = '100vh';
document.documentElement.style.minHeight = '100vh';
""" else ""}
"""
}
