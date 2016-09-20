package org.autorefactor.refactoring.rules.samples_out;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WakeLockSample extends Activity {
	private PowerManager.WakeLock wl;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLockSample");
		wl.acquire();
	}

	@Override
	protected void onPause() {
		wl.release();
		super.onPause();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
}

