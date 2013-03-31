package com.example.ece381;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity 
{
	EditText ipEdit;
	EditText portEdit;
	TextView infoText;
	String ipStr;
	String portStr;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup);
        
        ipEdit = (EditText) findViewById(R.id.ipEdit);
        portEdit = (EditText) findViewById(R.id.portEdit);
        infoText = (TextView) findViewById(R.id.infoText);
    }


    
    public void onConfirmClick(View v)
    {
    	 //ipStr = ipEdit.getEditableText().toString();
    	 //portStr = portEdit.getEditableText().toString();
    	ipStr = "192.168.1.70";
    	portStr = "50002";
		
		infoText.setText("IP entered: " + ipStr + "\n Port entered: " + portStr);

    }
    public void onClearClick(View v)
    {
		
    	ipEdit.setText(null);
    	portEdit.setText(null);

    }
    public void onStartClick(View v)
    {
//    	String ipStr = ipEdit.getEditableText().toString();
//    	String portStr = portEdit.getEditableText().toString();
 
//		ipStr="192.168.1.2";
        Intent intent = new Intent(this, Activity1.class); 
        
        intent.putExtra("ipStr+", ipStr);
        intent.putExtra("portStr+", portStr);

        startActivity(intent);
    }
}

