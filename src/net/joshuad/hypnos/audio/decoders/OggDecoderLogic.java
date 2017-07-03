package net.joshuad.hypnos.audio.decoders;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.sound.sampled.SourceDataLine;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

public class OggDecoderLogic {
	
	private BufferedInputStream encodedInput = null;
	private File file;
	
	private int bufferSize = 2048;
	private int count = 0;
	private int index = 0;
	private byte[] convertedBuffer;
	private int convertedBufferSize;

	private float[][][] pcmInfo;
	private int[] pcmIndex;
	private Packet joggPacket = new Packet();
	private Page joggPage = new Page();
	private StreamState joggStreamState = new StreamState();
	private SyncState joggSyncState = new SyncState();
	private DspState jorbisDspState = new DspState();
	private Block jorbisBlock = new Block( jorbisDspState );
	private Comment jorbisComment = new Comment();
	private Info jorbisInfo = new Info();
	
	private int totalBytesInStream = 0;
	
	public OggDecoderLogic ( File file ) throws IOException {
		this.file = file;
		encodedInput = new BufferedInputStream ( new FileInputStream ( file ) );

		joggSyncState.init();
		joggSyncState.buffer( bufferSize );
		
		boolean readHeader = readHeader();
		
		boolean initialized = initializeSound();
	}
	
	private boolean readHeader () {
		boolean needMoreData = true;

		int packet = 1;

		while ( needMoreData ) {
			// Read from the InputStream.
			try {
				count = encodedInput.read( joggSyncState.data, index, bufferSize );
			} catch ( IOException exception ) {
				System.err.println( "Could not read from the input stream." );
				System.err.println( exception );
			}

			// We let SyncState know how many bytes we read.
			joggSyncState.wrote( count );

			/*
			 * We want to read the first three packets. For the first packet, we
			 * need to initialize the StreamState object and a couple of other
			 * things. For packet two and three, the procedure is the same: we
			 * take out a page, and then we take out the packet.
			 */
			switch ( packet ) {
				// The first packet.
				case 1: {
					// We take out a page.
					switch ( joggSyncState.pageout( joggPage ) ) {
						// If there is a hole in the data, we must exit.
						case -1: {
							System.err.println( "There is a hole in the first " + "packet data." );
							return false;
						}

						// If we need more data, we break to get it.
						case 0: {
							break;
						}

						/*
						 * We got where we wanted. We have successfully read the
						 * first packet, and we will now initialize and reset
						 * StreamState, and initialize the Info and Comment
						 * objects. Afterwards we will check that the page
						 * doesn't contain any errors, that the packet doesn't
						 * contain any errors and that it's Vorbis data.
						 */
						case 1: {
							// Initializes and resets StreamState.
							joggStreamState.init( joggPage.serialno() );
							joggStreamState.reset();

							// Initializes the Info and Comment objects.
							jorbisInfo.init();
							jorbisComment.init();

							// Check the page (serial number and stuff).
							if ( joggStreamState.pagein( joggPage ) == -1 ) {
								System.err.println( "We got an error while " + "reading the first header page." );
								return false;
							}

							/*
							 * Try to extract a packet. All other return values
							 * than "1" indicates there's something wrong.
							 */
							if ( joggStreamState.packetout( joggPacket ) != 1 ) {
								System.err.println( "We got an error while " + "reading the first header packet." );
								return false;
							}

							/*
							 * We give the packet to the Info object, so that it
							 * can extract the Comment-related information,
							 * among other things. If this fails, it's not
							 * Vorbis data.
							 */
							if ( jorbisInfo.synthesis_headerin( jorbisComment, joggPacket ) < 0 ) {
								System.err.println( "We got an error while " + "interpreting the first packet. " + "Apparantly, it's not Vorbis data." );
								return false;
							}

							// We're done here, let's increment "packet".
							packet++;
							break;
						}
					}

					/*
					 * Note how we are NOT breaking here if we have proceeded to
					 * the second packet. We don't want to read from the input
					 * stream again if it's not necessary.
					 */
					if ( packet == 1 )
						break;
				}

				// The code for the second and third packets follow.
				case 2:
				case 3: {
					// Try to get a new page again.
					switch ( joggSyncState.pageout( joggPage ) ) {
						// If there is a hole in the data, we must exit.
						case -1: {
							System.err.println( "There is a hole in the second " + "or third packet data." );
							return false;
						}

						// If we need more data, we break to get it.
						case 0: {
							break;
						}

						/*
						 * Here is where we take the page, extract a packet and
						 * and (if everything goes well) give the information to
						 * the Info and Comment objects like we did above.
						 */
						case 1: {
							// Share the page with the StreamState object.
							joggStreamState.pagein( joggPage );

							/*
							 * Just like the switch(...packetout...) lines
							 * above.
							 */
							switch ( joggStreamState.packetout( joggPacket ) ) {
								// If there is a hole in the data, we must exit.
								case -1: {
									System.err.println( "There is a hole in the first" + "packet data." );
									return false;
								}

								// If we need more data, we break to get it.
								case 0: {
									break;
								}

								// We got a packet, let's process it.
								case 1: {
									/*
									 * Like above, we give the packet to the
									 * Info and Comment objects.
									 */
									jorbisInfo.synthesis_headerin( jorbisComment, joggPacket );

									// Increment packet.
									packet++;

									if ( packet == 4 ) {
										/*
										 * There is no fourth packet, so we will
										 * just end the loop here.
										 */
										needMoreData = false;
									}

									break;
								}
							}

							break;
						}
					}

					break;
				}
			}

			// We get the new index and an updated buffer.
			index = joggSyncState.buffer( bufferSize );

			/*
			 * If we need more data but can't get it, the stream doesn't contain
			 * enough information.
			 */
			if ( count == 0 && needMoreData ) {
				System.err.println( "Not enough header data was supplied." );
				return false;
			}
		}

		try {
			totalBytesInStream = encodedInput.available();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	void skipToPercent ( double percent ) {
		try {
			
			long bytesToSkip = (long)(totalBytesInStream * percent);
			
			encodedInput.skip( bytesToSkip );
			
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//TODO: Lol i have no idea how many frames this decodes. Whatever. If you figure it out rename it. 
	boolean processSomeFrames( SourceDataLine audioOutput ) {
		boolean needMoreData = true;
		switch ( joggSyncState.pageout( joggPage ) ) {
			// If there is a hole in the data, we just proceed.
			case -1: {
				//Non-fatal error
			}

			// If we need more data, we break to get it.
			case 0: {
				break;
			}

			// If we have successfully checked out a page, we continue.
			case 1: {
				// Give the page to the StreamState object.
				joggStreamState.pagein( joggPage );

				// If granulepos() returns "0", we don't need more data.
				if ( joggPage.granulepos() == 0 ) {
					needMoreData = false;
					break;
				}

				// Here is where we process the packets.
				processPackets: while ( true ) {
					switch ( joggStreamState.packetout( joggPacket ) ) {
						// Is it a hole in the data?
						case -1: {
							//Non-fatal error
						}

						// If we need more data, we break to get it.
						case 0: {
							break processPackets;
						}

						/*
						 * If we have the data we need, we decode the
						 * packet.
						 */
						case 1: {
							decodeCurrentPacket( audioOutput );
						}
					}
				}

				/*
				 * If the page is the end-of-stream, we don't need more
				 * data.
				 */
				if ( joggPage.eos() != 0 )
					needMoreData = false;
			}
		}

		// If we need more data...
		if ( needMoreData ) {
			// We get the new index and an updated buffer.
			index = joggSyncState.buffer( bufferSize );

			// Read from the InputStream.
			try {
				count = encodedInput.read( joggSyncState.data, index, bufferSize );
			} catch ( Exception e ) {
				System.err.println( e );
				return needMoreData;
			}

			// We let SyncState know how many bytes we read.
			joggSyncState.wrote( count );

			// There's no more data in the stream.
			if ( count == 0 ) {
				needMoreData = false;
			}
		}
		return needMoreData;
	}
	private boolean initializeSound () {

		convertedBufferSize = bufferSize * 2;
		convertedBuffer = new byte [ convertedBufferSize ];

		jorbisDspState.synthesis_init( jorbisInfo );

		jorbisBlock.init( jorbisDspState );

		/* TODO: Put this somewhere useful
		if ( !AudioSystem.isLineSupported( datalineInfo ) ) {
			System.err.println( "Audio output line is not supported." );
			return false;
		}
		*/

		pcmInfo = new float [ 1 ] [] [];
		pcmIndex = new int [ jorbisInfo.channels ];

		return true;
	}
	
	/**
	 * Decodes the current packet and sends it to the audio output line.
	 */
	private void decodeCurrentPacket ( SourceDataLine audioOutput ) {
		int samples;

		if ( jorbisBlock.synthesis( joggPacket ) == 0 ) {
			jorbisDspState.synthesis_blockin( jorbisBlock );
		}

		// We need to know how many samples to process.
		int range;

		/*
		 * Get the PCM information and count the samples. And while these
		 * samples are more than zero...
		 */
		while ( (samples = jorbisDspState.synthesis_pcmout( pcmInfo, pcmIndex )) > 0 ) {
			// We need to know for how many samples we are going to process.
			if ( samples < convertedBufferSize ) {
				range = samples;
			} else {
				range = convertedBufferSize;
			}

			// For each channel...
			for ( int i = 0; i < jorbisInfo.channels; i++ ) {
				int sampleIndex = i * 2;

				// For every sample in our range...
				for ( int j = 0; j < range; j++ ) {
					/*
					 * Get the PCM value for the channel at the correct
					 * position.
					 */
					int value = (int) (pcmInfo[0][i][pcmIndex[i] + j] * 32767);

					/*
					 * We make sure our value doesn't exceed or falls below
					 * +-32767.
					 */
					if ( value > 32767 ) {
						value = 32767;
					}
					if ( value < -32768 ) {
						value = -32768;
					}

					/*
					 * It the value is less than zero, we bitwise-or it with
					 * 32768 (which is 1000000000000000 = 10^15).
					 */
					if ( value < 0 )
						value = value | 32768;

					/*
					 * Take our value and split it into two, one with the last
					 * byte and one with the first byte.
					 */
					convertedBuffer[sampleIndex] = (byte) (value);
					convertedBuffer[sampleIndex + 1] = (byte) (value >>> 8);

					/*
					 * Move the sample index forward by two (since that's how
					 * many values we get at once) times the number of channels.
					 */
					sampleIndex += 2 * (jorbisInfo.channels);
				}
			}

			// Write the buffer to the audio output line.
			audioOutput.write( convertedBuffer, 0, 2 * jorbisInfo.channels * range );

			// Update the DspState object.
			jorbisDspState.synthesis_read( range );
		}
	}

	public int getChannels () {
		return jorbisInfo.channels;
	}
	
	public int getRate () {
		return jorbisInfo.rate;
	}
	
	public void close() {

		joggStreamState.clear();
		jorbisBlock.clear();
		jorbisDspState.clear();
		jorbisInfo.clear();
		joggSyncState.clear();
		
		try {
			if ( encodedInput != null ) {
				encodedInput.close();
			}
		} catch ( Exception e ) {
			//TODO: 
			e.printStackTrace();
		}
	}

}
