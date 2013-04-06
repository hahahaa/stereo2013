package com.example.stereoplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.example.stereoplayer.MyApplication.SocketSend;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class LoadPlaylist extends Activity {
	
	ListView listView;
	private ArrayList<HashMap<String, String>> allPlaylist;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.load_playlist);
		listView = (ListView) findViewById(R.id.loadList);
		allPlaylist = new ArrayList<HashMap<String, String>>();
		
		File f = getFilesDir(); 
		String[] files = f.list();
		for (int i = 0; i<files.length;i++)
			Log.i("files", files[i]);
		
		for(int i=0; i<files.length; i++)
		{
			HashMap<String,String> item = new HashMap<String,String>();
			item.put( "Playlist", files[i]);
			allPlaylist.add( item );
		}
		
		SimpleAdapter adapter = new SimpleAdapter( this, allPlaylist, R.layout.listviewforload, new String[] { "Playlist" },
				new int[] { R.id.textViewForLoading});
		
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{	
				
				Object obj = parent.getItemAtPosition(position);

				
				//Drawable block = getWallpaper();
				//ListView listView = (ListView) findViewById(R.id.listView);
				//listView.setSelector(R.drawable.list_background);
				//View rowView = listView.getChildAt(position);
				
				//listView.set
				//listView.setSelector(R.drawable.selectorv2);

				@SuppressWarnings("unchecked")
				HashMap<String,String> item = (HashMap<String, String>) obj;
				MyApplication app = (MyApplication)LoadPlaylist.this.getApplication();
				app.playlistTitle = item.get("Playlist");
				finish();
			}
		}
				);
	}

}
