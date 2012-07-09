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
	private boolean previewing = true;
	
	private QRCodeReader reader;
	
	
	private SurfaceView qrDisplay;
	private SurfaceHolder qrDisplayHolder;
	

	private View overlay;
	
	private int cameraWidth = 0;
	private int cameraHeight = 0;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /*
         * This SurfaceView will be used to display QR codes.
         */
        qrDisplay = (SurfaceView) findViewById(R.id.qr_display);
        qrDisplayHolder = qrDisplay.getHolder();
        qrDisplayHolder.addCallback(new SurfaceHolder.Callback() {
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				displayClipboard();
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width,
					int height) {
			}
		});
        
        /*
         * This view is used to provide the flash on a code read.
         */
        overlay = (View) findViewById(R.id.flash_overlay);
        
        /*
         * This is the qr code reader object that reads and decodes
         * qr codes.
         */
        reader = new QRCodeReader();
        
        /*
         * This surface view is used to display the preview.
         */
        preview = (SurfaceView) findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(
			new SurfaceHolder.Callback() {
				
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
			});
        		
        		
        
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
    }
    
    /**
     * returns the camera size with the maximum area with dimensions
     * less than those specified
     * @param width
     * @param height
     * @param params
     * @return
     * The camera size with the maximum area with dimensions less
     * than those specified.
     */
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
    
    /**
     * Set up the camera to fit the screen.
     * @param width
     * @param height
     */
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
    	
    	cameraWidth = size.width;
    	cameraHeight = size.height;
    	
    	camera.setParameters(params);
    	
    	
    	
    }
    
    private void startPreview() {
		camera.startPreview();
    	
    }
    
    private void stopPreview() {
		camera.stopPreview();
    	
    }
    

    
    @Override
    public void onPause() {
    	
    	Log.e("scan", "pause");
    	
    	// pause the camera
    	stopPreview();
    	camera.setPreviewCallback(null);
    	camera.release();
    	camera = null;
    	
    
    	
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	// set up the camera again
    	camera = Camera.open();
    	camera.setPreviewCallback(new PreviewScanner());
    	startPreview();
    	
    	if (!previewing) {
    		displayClipboard();
    	}
    	
    	if (qrDisplay.getVisibility() == View.VISIBLE) {
    		Log.e("code", "qrDisplay is visible");
    	}
    	
    	if (preview.getVisibility() == View.VISIBLE) {
    		Log.e("code", "preview is visible");
    	}
    	
    	Log.e("scan", "resume");
    	
    }
    
    /**
     * Display a QR Code encoding a string.
     * @param str
     * The string to encode.
     */
    private boolean displayQRCode(String str) {
    	

    	
    	// the canvas belonging to the qr code display
        Canvas canvas = qrDisplayHolder.lockCanvas();      
        
        if (canvas == null) {
        	return false;
        }
        
        // get the canvas dimensions
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        
        // make a writer
        QRCodeWriter encoder = new QRCodeWriter();
        
        // generate the code
        BitMatrix code = null;
        try {
			code = encoder.encode(str, BarcodeFormat.QR_CODE, canvasWidth, canvasHeight);
		} catch (WriterException e) {
			return false;
			
		}
        
        // make paints for black and white
        Paint white = new Paint();
        Paint black = new Paint();
        white.setColor(Color.WHITE);
        black.setColor(Color.BLACK);
        
        // get the dimensions of the code
        int height = code.getHeight();
        int width = code.getWidth();
        
        // print the code to the canvas
        for (int j = 0;j<height;j++) {        	
        	for (int i = 0;i<width;i++) {
        		
        		/*
        		 *  Get the current bit of the code.
        		 *  (Each square is represented as a bit
        		 *  with 1 corresponding to black and 0
        		 *  to white.)
        		 */
        		boolean bit = code.get(i, j);
        		
        		// set the paint colour
        		Paint paint;
        		if (bit) {
        			paint = black;
        		} else {
        			paint = white;
        		}
        		
        		// draw a single square of the code to the canvas
        		canvas.drawRect(i, j, i+1, j+1, paint);
        		
        	}
        }
        
        /* Post the canvas data to the surfaceview and actually
         * display the qr code.
         */
        qrDisplayHolder.unlockCanvasAndPost(canvas);
        
        return true;
    }
    
    // the alpha value of the flash overlay
    private int alpha = 0;
    // the minimum number of frames betnween flashes
    private int frameCount = 8;
  
    /**
     * This is a preview callback for the camera. On each frame
     * captured by the camera, a search will be made for a qr code.
     * If a qr code is found, it is decoded, and its value is placed
     * in the device's clipboard.
     * @author steve
     *
     */
    private class PreviewScanner implements Camera.PreviewCallback {
    
    	/*
    	 * Called on each frame
    	 * (non-Javadoc)
    	 * @see android.hardware.Camera.PreviewCallback#onPreviewFrame(byte[], android.hardware.Camera)
    	 */
		@Override
		public void onPreviewFrame(byte[] data, Camera c) {
			
			// avoid a null pointer exception
			if (camera == null) {
	    		return;
	    	}
			
			// alpha should never be negative
			if (alpha < 0) {
				alpha = 0;
			}
			
			/*
			 * If the alpha is non-negative, set the opacity of
			 * the overlay.
			 */
			
			if (alpha >= 0) {
				setOverlay(alpha);
			}
			
			/*
			 * If the alpha is positive, decrease it to produce
			 * a fade effect.
			 */
			if (alpha > 0) {
				alpha -= 64;
			}
			
			/*
			 *  Increment the frame count.
			 *  This is used so the screen doesn't flash multiple
			 *  times when multiple reads are done in quick
			 *  succession, which is usually the case.
			 */
			frameCount++;
		
			
			// create an image containing the preview data
			PlanarYUVLuminanceSource image = 
					new PlanarYUVLuminanceSource(data, 
												 cameraWidth,
												 cameraHeight, 
												 0, 
												 0, 
												 cameraWidth, 
												 cameraHeight, 
												 false);
			
			// binarize the image
			GlobalHistogramBinarizer binarizer = 
					new GlobalHistogramBinarizer(image);
			
			// make a bitmap of the binazide image
			BinaryBitmap bitmap = new BinaryBitmap(binarizer);
			
			/*
			 *  Try to decode the qr code in the image.
			 *  If there is no qr code in the image, just return.
			 */
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
			
			// store the decoded code in the clipboard
			storeInClipboard(res.getText());
			Log.e("scan", "success");
			
			// start the flash
			if (alpha == 0 && frameCount > 8) {
				alpha = 0xFF;
				frameCount = 0;
			}	
		}
    }
    
    /**
     * Sets the alpha value of the colour of the flash overlay.
     * This will display a white overlay at a given level of
     * opacity.
     * @param bright
     * The opacity of the overlay. Range: 0x00 - 0xFF
     */
    private void setOverlay(int bright) {
    	overlay.setBackgroundColor((bright << 24) | 0xFFFFFF);
    	
    }
    
    /**
     * Stores a string in the denvice's clipboard
     * @param data
     * The string to be stored in the clipboard
     */
    private void storeInClipboard(String data) {
    	ClipboardManager cm =  (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    	cm.setText(data);
    }
    
    /**
     * Encodes the clipboard contents in a qr code and displays it.
     */
    private void displayClipboard() {
    	ClipboardManager cm =  (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    	CharSequence clipboardData = cm.getText();
    	String clipboardString = clipboardData.toString();
    	
    	displayQRCode(clipboardString);
    }

    /**
     * Hides the qr code display and displays the preview.
     */
    private void usePreview() {
    	previewing = true;
    	preview.setVisibility(View.VISIBLE);
    	qrDisplay.setVisibility(View.INVISIBLE);
    }
    
    /**
     * Hides the preview and displays the qr display
     */
    private void useQRDisplay() {
    	previewing = false;
    	preview.setVisibility(View.INVISIBLE);
    	qrDisplay.setVisibility(View.VISIBLE);
    }
    
    /*
     * Catch presses of the menu button and toggle the qr code
     * display when it is pressed.
     * (non-Javadoc)
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
     
    	if (keyCode == KeyEvent.KEYCODE_MENU) {
    		if (scanning) {
    			useQRDisplay();
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