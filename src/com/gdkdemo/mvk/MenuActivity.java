package com.gdkdemo.mvk;

import org.hitlabnz.wailc.R;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class MenuActivity extends Activity {
	
	// open option menu immediately
	@Override
	public void onAttachedToWindow() {
        super.onAttachedToWindow();
		openOptionsMenu();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_close:
			// stop service
			stopService(new Intent(this, LiveCardMainService.class));
			return true; // we handled it
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	// finish activity when option menu closed 
	@Override
	public void onOptionsMenuClosed(Menu menu) {
		finish();
	}
	
	

}
