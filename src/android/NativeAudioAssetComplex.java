//
//
//  NativeAudioAssetComplex.java
//
//  Created by Sidney Bofah on 2014-06-26.
//

package com.rjfun.cordova.plugin.nativeaudio;

import java.io.IOException;
import java.util.concurrent.Callable;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import org.apache.cordova.CordovaInterface;
import android.content.Context;

public class NativeAudioAssetComplex implements OnPreparedListener, OnCompletionListener {

	private static final int INVALID = 0;
	private static final int PREPARED = 1;
	private static final int PENDING_PLAY = 2;
	private static final int PLAYING = 3;
	private static final int PENDING_LOOP = 4;
	private static final int LOOPING = 5;
	
	private MediaPlayer mp;
	private int state;
    Callable<Void> completeCallback;

    private AudioManager am;
    private int curVolume = 0;

	public NativeAudioAssetComplex( AssetFileDescriptor afd, float volume, CordovaInterface cordova)  throws IOException
	{
		state = INVALID;
		mp = new MediaPlayer();
        mp.setOnCompletionListener(this);
        mp.setOnPreparedListener(this);
		mp.setDataSource( afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
		mp.setAudioStreamType(AudioManager.STREAM_ALARM);
		mp.setVolume(volume, volume);
		mp.prepare();


		am = (AudioManager)cordova.getActivity().getSystemService(Context.AUDIO_SERVICE);
	}
	
	public void play(Callable<Void> completeCb) throws IOException
	{
        completeCallback = completeCb;
		invokePlay( false );
	}
	
	private void invokePlay( Boolean loop )
	{
		curVolume = am.getStreamVolume(AudioManager.STREAM_ALARM);
        am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

		Boolean playing = ( mp.isLooping() || mp.isPlaying() );
		if ( playing )
		{
			mp.pause();
			mp.setLooping(loop);
			mp.seekTo(0);
			mp.start();
		}
		if ( !playing && state == PREPARED )
		{
			state = (loop ? PENDING_LOOP : PENDING_PLAY);
			onPrepared( mp );
		}
		else if ( !playing )
		{
			state = (loop ? PENDING_LOOP : PENDING_PLAY);
			mp.setLooping(loop);
			mp.start();
		}
	}

	public boolean pause()
	{
		try
		{
    				if ( mp.isLooping() || mp.isPlaying() )
				{
					mp.pause();
					return true;
				}
        	}
		catch (IllegalStateException e)
		{
		// I don't know why this gets thrown; catch here to save app
		} finally {
			am.setStreamVolume(AudioManager.STREAM_ALARM, curVolume, 0);
		}
		return false;
	}

	public void resume()
	{
		mp.start();
	}

    public void stop()
	{
		try
		{
			if ( mp.isLooping() || mp.isPlaying() )
			{
				state = INVALID;
				mp.pause();
				mp.seekTo(0);
	           	}
		}
	        catch (IllegalStateException e)
	        {
            // I don't know why this gets thrown; catch here to save app
	        } finally {
			am.setStreamVolume(AudioManager.STREAM_ALARM, curVolume, 0);
		}
	}

	public void setVolume(float volume) 
	{
	        try
	        {
			mp.setVolume(volume,volume);
            	}
            	catch (IllegalStateException e) 
		{
                // I don't know why this gets thrown; catch here to save app
		}
	}
	
	public void loop() throws IOException
	{
		invokePlay( true );
	}
	
	public void unload() throws IOException
	{
		this.stop();
		mp.release();
	}
	
	public void onPrepared(MediaPlayer mPlayer) 
	{
		if (state == PENDING_PLAY) 
		{
			mp.setLooping(false);
			mp.seekTo(0);
			mp.start();
			state = PLAYING;
		}
		else if ( state == PENDING_LOOP )
		{
			mp.setLooping(true);
			mp.seekTo(0);
			mp.start();
			state = LOOPING;
		}
		else
		{
			state = PREPARED;
			mp.seekTo(0);
		}
	}
	
	public void onCompletion(MediaPlayer mPlayer)
	{
		if (state != LOOPING)
		{
			this.state = INVALID;
			try {
				this.stop();
                completeCallback.call();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
