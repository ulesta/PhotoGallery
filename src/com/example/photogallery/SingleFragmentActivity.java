package com.example.photogallery;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public abstract class SingleFragmentActivity extends FragmentActivity {
	
	protected abstract Fragment createFragment();
	
	// now subclasses can choose to override this to return a diff layout
	protected int getLayoutResId() {
		return R.layout.activity_fragment;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(getLayoutResId());
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
		
		if ( fragment == null ) {
			fragment = createFragment();
			/* for fragments to get arguments, fragments must be 
				created before adding to fragmentmanager */
			fm.beginTransaction()
				.add(R.id.fragmentContainer, fragment)
				.commit();
		} 
	}
	
}
