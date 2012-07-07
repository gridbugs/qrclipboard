package app.qrclipboard;
import java.io.IOException;
import java.util.Formatter;


import com.google.zxing.client.android.PlanarYUVLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;


import com.google.zxing.qrcode.*;


import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;


import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;


import android.text.ClipboardManager;
import android.util.Log;

public class QRClipboardActivity extends Activity {
	
	
	
	private Camera camera;
	
	private SurfaceView preview;
	private SurfaceHolder previewHolder;
	
	private boolean previewing = false;
	
	private QRCodeReader reader;
	
	Formatter formatter;
	
	SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
		
		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			Log.e("scan", "destroyed");
			
		}
		
		@Override
		public void surfaceCreated(SurfaceHolder arg0) {
			// TODO Auto-generated method stub
			
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
        
        // fix up rotation
        /*
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.rotation);
        LayoutAnimationController animController = new LayoutAnimationController(anim, 0);
        LinearLayout layout = (LinearLayout) findViewById(R.id.overlay);
        layout.setLayoutAnimation(animController);
        */
        
        
        
        Log.e("scan", "hello");
        
        formatter = new Formatter();
        
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
    	
    	Log.e("configureCamera", 
    			formatter.format("%d %d", size.width, size.height).toString());
    	
    	
    }
    
    private void startPreview() {
		camera.startPreview();
		previewing = true;
    	
    }
    
    private void stopPreview() {
		camera.stopPreview();
		previewing = false;
    	
    }
    

    
    @Override
    public void onPause() {
    	
    	
    	camera.setPreviewCallback(null);
    	camera.stopPreview();
    	
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
    	
    }
    

    public void startScanning(View view) {
    	
    	SurfaceView display = (SurfaceView) findViewById(R.id.display);
        SurfaceHolder displayHolder = display.getHolder();
        Canvas canvas = displayHolder.lockCanvas();
        
        canvas.drawARGB(255, 0, 255, 0);
        
        displayHolder.unlockCanvasAndPost(canvas);
        
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
			
			//scanResult = res.getText();
			storeInClipboard(res.getText());
			Log.e("scan", "success");
			
			if (alpha == 0 && frameCount > 8) {
				alpha = 0xFF;
				frameCount = 0;
			}
			
			
				
			
		}
    	
    }
    
    private void setOverlay(int bright) {
    	View overlay = (View) findViewById(R.id.overlay);
    	overlay.setBackgroundColor((bright << 24) | 0xFFFFFF);
    	
    }
    
    private void storeInClipboard(String data) {
    	ClipboardManager cm =  (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
    	cm.setText(data);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
	
    	return true;
    	
    }

}