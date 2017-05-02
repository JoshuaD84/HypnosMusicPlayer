package players;

/* 
 * Simple GUI FLAC player
 * 
 * Copyright (c) 2017 Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/simple-gui-flac-player-java
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 */

import java.io.*;
import java.nio.file.Path;
import javax.sound.sampled.*;


/**
 * A GUI application which lets you open a FLAC file, listen to the audio, and seek to positions
 * in the file. Run this program with no command line arguments: java SimpleGuiFlacPlayer.
 */
public final class FLACPlayer {
	
	private double positionPercent = 0; 
	private File openRequest = null;
	private double seekRequest = -1;  // Either -1 or a number in [0.0, 1.0]
	
	// Decoder state
	private static FlacDecoder decoder = null;
	private static SourceDataLine line = null;
	private static long clipStartTime;
	
	public FLACPlayer ( ) {
		Thread t = new Thread ( new Runnable() {
			public void run() {
				doAudioDecoderWorkerLoop();
			}
		});
		t.start();
	}
	
	public void playFlac ( Path flacLocation ) {
		openRequest = flacLocation.toAbsolutePath().toFile();
	}
	
	public double getPositionPercent() {
		return positionPercent;
	}
	
	public void seekRequest ( double percent ) {
		seekRequest = percent; //(double)slider.getValue() / slider.getMaximum();
	}
	
		
	private void doAudioDecoderWorkerLoop() {
		while (true) {
			try {
				File openReq;
				double seekReq;
				
				openReq = openRequest;
				openRequest = null;
				seekReq = seekRequest;
				seekRequest = -1;
				
				
				// Open or switch files, and start audio line
				if (openReq != null) {
					seekReq = -1;
					closeFile();
					decoder = new FlacDecoder(openReq);
					if (decoder.numSamples == 0) {
						throw new FlacDecoder.FormatException("Unknown audio length");
					}
					
					AudioFormat format = new AudioFormat(decoder.sampleRate, decoder.sampleDepth, decoder.numChannels, true, false);
					line = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
					line.open(format);
					line.start();
					
					
					clipStartTime = 0;
				} else if (decoder == null) {
					return;
				}
				
				// Decode next audio block, or seek and decode
				long[][] samples = null;
				if (seekReq == -1) {
					Object[] temp = decoder.readNextBlock();
					if (temp != null)
						samples = (long[][])temp[0];
				} else {
					long samplePos = Math.round(seekReq * decoder.numSamples);
					samples = decoder.seekAndReadBlock(samplePos);
					line.flush();
					clipStartTime = line.getMicrosecondPosition() - Math.round(samplePos * 1e6 / decoder.sampleRate);
				}
				
				// Set display position
				double timePos = (line.getMicrosecondPosition() - clipStartTime) / 1e6;
				positionPercent = timePos * decoder.sampleRate / decoder.numSamples;
				
				// Wait when end of stream reached
				if (samples == null) {
					return;
				}
				
				// Convert samples to channel-interleaved bytes in little endian
				int bytesPerSample = decoder.sampleDepth / 8;
				byte[] sampleBytes = new byte[samples[0].length * samples.length * bytesPerSample];
				for (int i = 0, k = 0; i < samples[0].length; i++) {
					for (int ch = 0; ch < samples.length; ch++) {
						long val = samples[ch][i];
						for (int j = 0; j < bytesPerSample; j++, k++)
							sampleBytes[k] = (byte)(val >>> (j << 3));
					}
				}
				line.write(sampleBytes, 0, sampleBytes.length);
			} catch (IOException|LineUnavailableException e) {
				e.printStackTrace(); 
				try {
					closeFile();
				} catch (IOException ee) {
					ee.printStackTrace();
					System.exit(1);
				}
			} 
		}
	}
	
	private static void closeFile() throws IOException {
		if (decoder != null) {
			decoder.close();
		}
		if (line != null) {
			line.close();
		}
	}
	
}
	
	
	
	
	
	
	
	
	
	
	
	
