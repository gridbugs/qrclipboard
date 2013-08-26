
function drawQR(str) {

    var canvas = document.getElementById("canvas");

    var qr = new JSQR();
    var code = new qr.Code();

    code.encodeMode = code.ENCODE_MODE.BYTE;
    code.versien = code.DEFAULT;
    code.errorCorrection = code.ERROR_CORRECTION.H;

    var input = new qr.Input();
    input.dataType = input.DATA_TYPE.TEXT;
    input.data = str;

    var matrix = new qr.Matrix(input, code);
    matrix.scale = 8;
    matrix.margin = 2;

    canvas.setAttribute('width', matrix.pixelWidth);
    canvas.setAttribute('height', matrix.pixelWidth);

    canvas.getContext('2d').fillStyle = 'rgb(0,0,0)';

    matrix.draw(canvas, 0, 0);

    var resizer = document.getElementById("resizer");
    resizer.style.width = matrix.pixelWidth + "px";
    resizer.style.height = matrix.pixelWidth + "px";


}

function run() {

    var clipboardData = paste();
    drawQR(clipboardData);
}

function copy(str) {
    var sandbox = $('#sandbox').val(str).select();
    document.execCommand('copy');
    sandbox.val('');
}

function paste(str) {

    var sandboxElement = document.getElementById("sandbox");
    sandboxElement.style.display = "block";

    var result = '';
    var sandbox = $('#sandbox').val('').select();

    if (document.execCommand('paste')) {
        result = sandbox.val();
    }

    sandbox.val('');
    sandboxElement.style.display = "none";
    return result;
}

document.addEventListener('DOMContentLoaded', run);
