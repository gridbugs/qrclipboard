package app.qrclipboard;
import java.io.IOException;


import com.google.zxing.client.android.PlanarYUVLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;


import com.google.zxing.qrcode.*;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;


import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


import android.text.ClipboardManager;
import android.util.Log;

public class QRClipboardActivity extends Activity {
	
	
	
	private Camera camera;
	
	private SurfaceView preview;
	private SurfaceHolder previewHolder;
	
	private boolean scanning =  true;
	
	private QRCodeReader reader;
	
	
	SurfaceView display;
	SurfaceHolder displayHolder;
	
	
	SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
		
		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			Log.e("scan", "destroyed");
			
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder arg0) {
			
			
		}
		 
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
			configureCamera(width, height);

		    
			
			startPreview();
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        
        display = (SurfaceView) findViewById(R.id.display);
        displayHolder = display.getHolder();

        
        
        reader = new QRCodeReader();
        
        preview = (SurfaceView) findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(callback);
        
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
    private Camera.Size getCameraSize(int width, int height, Camera.Parameters params) {
    	
    	int bestArea = 0;
    	Camera.Size result = null;
    	
    	for (Camera.Size size : params.getSupportedPreviewSizes()) {
    		if (size.width <= width && size.height <= height) {
    			int area = size.width * size.height;
    			if (area > bestArea) {
    				result = size;
    				bestArea = area;
    			}
    		}
    	}
    	
    	return result;
    }
    
    private void configureCamera(int width, int height) {
    	
    	
    	try {
			camera.setPreviewDisplay(previewHolder);
		} catch (IOException e) {
			Log.e("configureCamera", "couldn't set preview display");
			e.printStackTrace();
		}
    	
    	camera.setPreviewCallback(new PreviewScanner());
    	
    	
    	Camera.Parameters params = camera.getParameters();
    	Camera.Size size = getCameraSize(width, height, params);
    	params.setPreviewSize(size.width, size.height);
    	
    	
    	camera.setParameters(params);
    	
    	
    	
    }
    
    private void startPreview() {
		camera.startPreview();
    	
    }
    
    private void stopPreview() {
		camera.stopPreview();
		usePreview();
    	
    }
    

    
    @Override
    public void onPause() {
    	
    	Log.e("scan", "pause");
    	
    	stopPreview();
    	
    	camera.setPreviewCallback(null);
    	
    	
    	camera.release();
    	camera = null;
    	
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	
    	
    	camera = Camera.open();
    	camera.setPreviewCallback(new PreviewScanner());
    	startPreview();
    	
    	
    	scanning = true;
    	
    	Log.e("scan", "resume");
    	
    }
    
    private void displayQRCode(String str) {
    	
    	useDisplay();    	
    	
        Canvas canvas = displayHolder.lockCanvas();        
        
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        
        QRCodeWriter encoder = new QRCodeWriter();
        BitMatrix code = null;
        try {
			code = encoder.encode(str, BarcodeFormat.QR_CODE, canvasWidth, canvasHeight);
		} catch (WriterException e) {
			e.printStackTrace();
			
		}
        
        Paint white = new Paint();
        Paint black = new Paint();
        
        white.setColor(Color.WHITE);
        black.setColor(Color.BLACK);
        
        int height = code.getHeight();
        int width = code.getWidth();
        
        for (int j = 0;j<height;j++) {        	
        	for (int i = 0;i<width;i++) {
        		
        		boolean bit = code.get(i, j);
        		Paint paint;
        		
        		if (bit) {
        			paint = black;
        		} else {
        			paint = white;
        		}
        		
        		canvas.drawRect(i, j, i+1, j+1, paint);
        		
        	}
        }
        
        
        displayHolder.unlockCanvasAndPost(canvas);
    }
    

    public void copyButtonClick(View view) {
    	displayClipboard();
    }
  
    private int alpha = 0;
    private int frameCount = 8;
  
    private class PreviewScanner implements Camera.PreviewCallback {
    
    	
		@Override
		public void onPreviewFrame(byte[] data, Camera c) {
			if (camera == null) {
	    		return;
	    	}
			
			if (alpha < 0) {
				alpha = 0;
			}
			
			if (alpha >= 0) {
				setOverlay(alpha);
			}
			
			if (alpha > 0) {
				alpha -= 64;
			}
			
			
			
			frameCount++;
		
			Camera.Parameters params = camera.getParameters();
			Camera.Size size = params.getPreviewSize();
			
			PlanarYUVLuminanceSource image = 
					new PlanarYUVLuminanceSource(data, 
												 size.width,
												 size.height, 
												 0, 
												 0, 
												 size.width, 
												 size.height, 
												 false);
			
			GlobalHistogramBinarizer binarizer = 
					new GlobalHistogramBinarizer(image);
			BinaryBitmap bitmap = new BinaryBitmap(binarizer);
			Result res = null;
			try {
				res = reader.decode(bitmap);
			} catch (NotFoundException e) {
				return;
			} catch (ChecksumException e) {
				return;
			} catch (FormatException e) {
				return;
			}
			
			storeInClipboard(res.getText());
			Log.e("scan", "success");
			
			if (alpha == 0 && frameCount > 8) {
				alpha = 0xFF;
				frameCount = 0;
			}	
		}
    }
    
    private void setOverlay(int bright) {
    	View overlay = (View) findViewById(R.id.flash_overlay);
    	overlay.setBackgroundColor((bright << 24) | 0xFFFFFF);
    	
    }
    
    private void storeInClipboard(String data) {
    	ClipboardManager cm =  (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    	cm.setText(data);
    }
    
    private void displayClipboard() {
    	ClipboardManager cm =  (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    	CharSequence clipboardData = cm.getText();
    	String clipboardString = clipboardData.toString();
    	displayQRCode(clipboardString);
    }
    

    
    private void usePreview() {
    	preview.setVisibility(View.VISIBLE);
    	display.setVisibility(View.INVISIBLE);
    }
    
    private void useDisplay() {
    	preview.setVisibility(View.INVISIBLE);
    	display.setVisibility(View.VISIBLE);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
     
    	if (keyCode == KeyEvent.KEYCODE_MENU) {
            Log.e("code", "code");
    		if (scanning) {
    			displayClipboard();
    			scanning = false;
    		} else {
    			usePreview();
    			scanning = true;
    		}
    		
            return true;
        }
        
        
        return super.onKeyDown(keyCode, event);
    }

}