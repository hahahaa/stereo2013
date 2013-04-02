package com.example.drrrrraag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainActivity extends Activity {

	private ArrayList<HashMap<String, String>> allSonglist;	// input from the caller
	private ArrayList<HashMap<String, String>> newSongList;	// send this to middle man
	private List<String> droppedList;	// a list for displaying songs in newSongList
	
	private ListView listSource;
	private ListView listTarget;
	private LinearLayout targetLayout;
	private ArrayAdapter<String> targetAdapter;
	
	private EditText nameEdit;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dragdropplaylist);
		listSource = (ListView) findViewById(R.id.listView1);
		listTarget = (ListView) findViewById(R.id.listView2);
		targetLayout = (LinearLayout)findViewById(R.id.targetlayout);
		nameEdit = (EditText)findViewById(R.id.editText1);

		allSonglist = gethardCodedList();	// TODO remove it  initialize allSonglist from the caller
		newSongList = new ArrayList<HashMap<String, String>>();

		listSource.setTag("listSource");
		listTarget.setTag("listTarget");
		targetLayout.setTag("targetLayout");
		
		listSource.setAdapter(new SimpleAdapter( this, allSonglist, R.layout.dragdropsourcelistview, new String[] { "Song","Artist" },
				new int[] { R.id.textView1, R.id.textView2}));
		
		listSource.setOnItemLongClickListener(new sourceListItemLongClickListener());
		listTarget.setOnItemLongClickListener(new targetListItemLongClickListener());
		
		droppedList = new ArrayList<String>();			
		targetAdapter = new ArrayAdapter<String>(this,
	              android.R.layout.simple_list_item_1, droppedList);
		
		listTarget.setAdapter(targetAdapter);

		listSource.setOnDragListener(new MyDragEventListener());
		targetLayout.setOnDragListener(new MyDragEventListener());

	}
	
	/**
	 * send the created list
	 */
	public void sendList (View view) {
		if(!newSongList.isEmpty()) {
			// TODO	send newSongList	newSongList only contain the song ID
			// TODO do sth with the listName
			String listName = nameEdit.getEditableText().toString();
			if(listName.compareTo("") == 0)
				listName = "newlist1";
		}
	}
	
	private static class MyDragShadowBuilder extends View.DragShadowBuilder {
		private static Drawable shadow;

		public MyDragShadowBuilder(View v) {
			super(v);
			shadow = new ColorDrawable(Color.LTGRAY);
		}

		@Override
		public void onProvideShadowMetrics (Point size, Point touch){
			int width = getView().getWidth();
			int height = getView().getHeight();

			shadow.setBounds(0, 0, width, height);
			size.set(width, height);
			touch.set(width / 2, height / 2);
		}

		@Override
		public void onDrawShadow(Canvas canvas) {
			shadow.draw(canvas);
		}

	}

	class sourceListItemLongClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> l, View v,
				int position, long id) {
			Log.i("drag","sourceListItemLongClickListener");
			Log.i("drag", allSonglist.get(position).toString());
			//Selected item is passed as item in dragData
			ClipData.Item item = new ClipData.Item(allSonglist.get(position).toString());

			String[] clipDescription = {ClipDescription.MIMETYPE_TEXT_PLAIN};
			ClipData dragData = new ClipData((CharSequence)v.getTag(),
					clipDescription,
					item);
			DragShadowBuilder myShadow = new MyDragShadowBuilder(v);

			v.startDrag(dragData, //ClipData
					myShadow,  //View.DragShadowBuilder
					position,  //Object myLocalState
					0);    //flags
			return true;
		}
	}
	
	class targetListItemLongClickListener implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id) {		
			droppedList.remove(position);
			newSongList.remove(position);
			targetAdapter.notifyDataSetChanged();
			
			Log.i("drag", "size of new list: " + newSongList.size());
			Log.i("drag", newSongList.toString());
			return true;
		}
	}

	protected class MyDragEventListener implements View.OnDragListener {
		@Override
		public boolean onDrag(View v, DragEvent event) {
			final int action = event.getAction();
			String commentMsg;
			switch(action) {
			case DragEvent.ACTION_DRAG_STARTED:
				//All involved view accept ACTION_DRAG_STARTED for MIMETYPE_TEXT_PLAIN
				if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					return true; //Accept
				} else {
					return false; //reject
				}
			case DragEvent.ACTION_DRAG_ENTERED:
				return true;
			case DragEvent.ACTION_DRAG_LOCATION:
				return true;
			case DragEvent.ACTION_DRAG_EXITED:
				return true;
			case DragEvent.ACTION_DROP:
				//If apply only if drop on buttonTarget
				if(v == targetLayout) {
					int position = (Integer) event.getLocalState();
					
					HashMap<String, String> song = new HashMap<String, String>();
					song.put("Song", allSonglist.get(position).get("Song"));
					song.put("Artist", allSonglist.get(position).get("Artist"));
					newSongList.add(song);
					
					String tmp = song.get("Song") + "\n" + song.get("Artist");
					droppedList.add(tmp);
					targetAdapter.notifyDataSetChanged();
					
					commentMsg = "Dropped item";
					Log.i("drag", commentMsg);
					return true;
				}else {
					return false;
				}
			case DragEvent.ACTION_DRAG_ENDED:
				if (event.getResult()){
					commentMsg = v.getTag() + " : ACTION_DRAG_ENDED - success." + " size of new list: " + newSongList.size();
					Log.i("drag", commentMsg);
					Log.i("drag", newSongList.toString());

				} else {
					commentMsg = v.getTag() + " : ACTION_DRAG_ENDED - fail.";
					Log.i("drag", commentMsg);
				}
				return true;
			default: //unknown case
				commentMsg = v.getTag() + " : UNKNOWN !!!";
				Log.i("drag", commentMsg);
				return false;
			}
		} 
	}
	
	private ArrayList<HashMap<String, String>> gethardCodedList() {
		ArrayList<HashMap<String, String>> lis = new ArrayList<HashMap<String, String>>();

		HashMap<String,String> item1 = new HashMap<String,String>(); 
		item1.put( "Song", "song1" ); 
		item1.put( "Artist", "artist1" ); 
		lis.add( item1 );
		HashMap<String,String> item2 = new HashMap<String,String>(); 
		item2.put( "Song", "song2" ); 
		item2.put( "Artist", "artist2" ); 
		lis.add( item2 );
		HashMap<String,String> item3 = new HashMap<String,String>(); 
		item3.put( "Song", "song3" ); 
		item3.put( "Artist", "artist3" ); 
		lis.add( item3 );

		return lis; 
	}
} 