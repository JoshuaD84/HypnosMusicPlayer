package net.joshuad.musicplayer.players;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

/**
 * Decompresses an Ogg file.
 * <p>
 * How to use:
 * <ol>
 * <li>Create OggInputStream passing in the input stream of the packed ogg file.
 * <li>Fetch format and sampling rate using getFormat() and getRate(). Use it to
 * initalize the sound player.
 * <li>Read the PCM data using one of the read functions, and feed it to your
 * player.
 * <p>
 * OggInputStream provides a read(ByteBuffer, int, int) that can be used to read
 * data directly into a native buffer.
 * <p>
 * This implementation was taken (freely) from
 * <a href="http://home.halden.net/tombr/ogg/ogg.html">
 * http://home.halden.net/tombr/ogg/ogg.html</a>.
 * 
 * @author pvg, tombr
 */
public class OggInputStream extends FilterInputStream {

	/** The mono 16 bit format */
	public static final int FORMAT_MONO16 = 1;

	/** The stereo 16 bit format */
	public static final int FORMAT_STEREO16 = 2;

	// temp vars
	private float[][][] _pcm = new float [ 1 ] [] [];

	private int[] _index;

	// end of stream
	private boolean eos = false;

	// sync and verify incoming physical bitstream
	private SyncState syncState = new SyncState();

	// take physical pages, weld into a logical stream of packets
	private StreamState streamState = new StreamState();

	// one Ogg bitstream page. Vorbis packets are inside
	private Page page = new Page();

	// one raw packet of data for decode
	private Packet packet = new Packet();

	// struct that stores all the static vorbis bitstream settings
	private Info info = new Info();

	// struct that stores all the bitstream user comments
	private Comment comment = new Comment();

	// central working state for the packet->PCM decoder
	private DspState dspState = new DspState();

	// local working space for packet->PCM decode
	private Block block = new Block( dspState );

	/// Conversion buffer size
	private static int convsize = 4096 * 2;

	// Conversion buffer
	private static byte[] convbuffer = new byte [ convsize ];

	// where we are in the convbuffer
	private int convbufferOff = 0;

	// bytes ready in convbuffer.
	private int convbufferSize = 0;

	// a dummy used by read() to read 1 byte.
	private byte readDummy[] = new byte [ 1 ];

	/**
	 * Creates an OggInputStream that decompressed the specified ogg file.
	 * 
	 * @param input
	 *            an input stream that reads ogg vorbis data
	 */
	public OggInputStream ( InputStream input ) {
		super( input );

		try {
			initVorbis();
			_index = new int [ info.channels ];
		} catch ( Exception e ) {
			e.printStackTrace();
			eos = true;
		}
	}

	/**
	 * Gets the format of the ogg file. Is either FORMAT_MONO16 or
	 * FORMAT_STEREO16
	 * 
	 * @return one of {@link OggInputStream#FORMAT_MONO16} or
	 *         {@link #FORMAT_STEREO16}.
	 */
	public int getFormat () {
		if ( info.channels == 1 ) {
			return FORMAT_MONO16;
		} else {
			return FORMAT_STEREO16;
		}
	}

	/**
	 * Gets the rate of the pcm audio.
	 * 
	 * @return sample rate of the ogg stream
	 */
	public int getRate () {
		return info.rate;
	}

	/**
	 * Reads the next byte of data from this input stream. The value byte is
	 * returned as an int in the range 0 to 255. If no byte is available because
	 * the end of the stream has been reached, the value -1 is returned. This
	 * method blocks until input data is available, the end of the stream is
	 * detected, or an exception is thrown.
	 * 
	 * @return the next byte of data, or -1 if the end of the stream is reached.
	 * @throws IOException
	 */
	@Override
	public int read () throws IOException {
		int retVal = read( readDummy, 0, 1 );
		return (retVal == -1 ? -1 : readDummy[0]);
	}

	/**
	 * Reads up to len bytes of data from the input stream into an array of
	 * bytes.
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @param off
	 *            the start offset of the data.
	 * @param len
	 *            the maximum number of bytes read.
	 * @return the total number of bytes read into the buffer, or -1 if there is
	 *         no more data because the end of the stream has been reached.
	 * @throws IOException
	 */
	@Override
	public int read ( byte b[], int off, int len ) throws IOException {
		if ( eos ) {
			return -1;
		}

		int bytesRead = 0;
		while ( !eos && (len > 0) ) {
			fillConvbuffer();

			if ( !eos ) {
				int bytesToCopy = Math.min( len, convbufferSize - convbufferOff );
				System.arraycopy( convbuffer, convbufferOff, b, off, bytesToCopy );
				convbufferOff += bytesToCopy;
				bytesRead += bytesToCopy;
				len -= bytesToCopy;
				off += bytesToCopy;
			}
		}

		return bytesRead;
	}

	/**
	 * Reads up to len bytes of data from the input stream into a ByteBuffer.
	 * 
	 * @param b
	 *            the buffer into which the data is read.
	 * @param off
	 *            the start offset of the data.
	 * @param len
	 *            the maximum number of bytes read.
	 * @return the total number of bytes read into the buffer, or -1 if there is
	 *         no more data because the end of the stream has been reached.
	 * @throws IOException
	 */
	public int read ( ByteBuffer b, int off, int len ) throws IOException {
		if ( eos ) {
			return -1;
		}

		b.position( off );
		int bytesRead = 0;
		while ( !eos && (len > 0) ) {
			fillConvbuffer();

			if ( !eos ) {
				int bytesToCopy = Math.min( len, convbufferSize - convbufferOff );
				b.put( convbuffer, convbufferOff, bytesToCopy );
				convbufferOff += bytesToCopy;
				bytesRead += bytesToCopy;
				len -= bytesToCopy;
			}
		}

		return bytesRead;
	}

	/**
	 * Helper function. Decodes a packet to the convbuffer if it is empty.
	 * Updates convbufferSize, convbufferOff, and eos.
	 * 
	 * @throws IOException
	 */
	private void fillConvbuffer () throws IOException {
		if ( convbufferOff >= convbufferSize ) {
			convbufferSize = lazyDecodePacket();
			convbufferOff = 0;
			if ( convbufferSize == -1 ) {
				eos = true;
			}
		}
	}

	/**
	 * Returns 0 after EOF is reached, otherwise always return 1.
	 * <p>
	 * Programs should not count on this method to return the actual number of
	 * bytes that could be read without blocking.
	 * 
	 * @return 1 before EOF and 0 after EOF is reached.
	 * @throws IOException
	 */
	@Override
	public int available () throws IOException {
		return (eos ? 0 : 1);
	}

	/**
	 * OggInputStream does not support mark and reset. This function does
	 * nothing.
	 * 
	 * @throws IOException
	 */
	@Override
	public void reset () throws IOException {
	}

	/**
	 * OggInputStream does not support mark and reset.
	 * 
	 * @return false.
	 */
	@Override
	public boolean markSupported () {
		return false;
	}

	/**
	 * Skips over and discards n bytes of data from the input stream. The skip
	 * method may, for a variety of reasons, end up skipping over some smaller
	 * number of bytes, possibly 0. The actual number of bytes skipped is
	 * returned.
	 * 
	 * @param n
	 *            the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @throws IOException
	 */
	@Override
	public long skip ( long n ) throws IOException {

		int bytesToRead = (int)n;
		
		int totalBytesRead = 0;
		
		while ( true ) {
			byte[] dump;
			if ( bytesToRead < 1024 ) {
				dump = new byte [ bytesToRead ];
				bytesToRead = 0;
			} else {
				dump = new byte [ 1024 ];
				bytesToRead -= 1024;
			}
			
			int bytesRead = this.read( dump );
			totalBytesRead += bytesRead;
			
			if ( bytesRead < dump.length ) {
				return totalBytesRead;
			}
			
			if ( bytesToRead <= 0 ) {
				break;
			}
		}

		return totalBytesRead;
	}

	public long skipRaw ( long n ) throws IOException {
		return in.skip( n );
	}
	/**
	 * Initalizes the vorbis stream. Reads the stream until info and comment are
	 * read.
	 * 
	 * @throws Exception
	 */
	private void initVorbis () throws Exception {
		// Now we can read pages
		syncState.init();

		// grab some data at the head of the stream. We want the first page
		// (which is guaranteed to be small and only contain the Vorbis
		// stream initial header) We need the first page to get the stream
		// serialno.

		// submit a 4k block to libvorbis' Ogg layer
		int index = syncState.buffer( 4096 );
		byte buffer[] = syncState.data;
		int bytes = in.read( buffer, index, 4096 );
		syncState.wrote( bytes );

		// Get the first page.
		if ( syncState.pageout( page ) != 1 ) {
			// have we simply run out of data? If so, we're done.
			if ( bytes < 4096 )
				return;// break;

			// error case. Must not be Vorbis data
			throw new Exception( "Input does not appear to be an Ogg bitstream." );
		}

		// Get the serial number and set up the rest of decode.
		// serialno first; use it to set up a logical stream
		streamState.init( page.serialno() );

		// extract the initial header from the first page and verify that the
		// Ogg bitstream is in fact Vorbis data

		// I handle the initial header first instead of just having the code
		// read all three Vorbis headers at once because reading the initial
		// header is an easy way to identify a Vorbis bitstream and it's
		// useful to see that functionality seperated out.

		info.init();
		comment.init();
		if ( streamState.pagein( page ) < 0 ) {
			// error; stream version mismatch perhaps
			throw new Exception( "Error reading first page of Ogg bitstream data." );
		}

		if ( streamState.packetout( packet ) != 1 ) {
			// no page? must not be vorbis
			throw new Exception( "Error reading initial header packet." );
		}

		if ( info.synthesis_headerin( comment, packet ) < 0 ) {
			// error case; not a vorbis header
			throw new Exception( "This Ogg bitstream does not contain Vorbis audio data." );
		}

		// At this point, we're sure we're Vorbis. We've set up the logical
		// (Ogg) bitstream decoder. Get the comment and codebook headers and
		// set up the Vorbis decoder

		// The next two packets in order are the comment and codebook headers.
		// They're likely large and may span multiple pages. Thus we read
		// and submit data until we get our two packets, watching that no
		// pages are missing. If a page is missing, error out; losing a
		// header page is the only place where missing data is fatal.

		int i = 0;
		while ( i < 2 ) {
			while ( i < 2 ) {

				int result = syncState.pageout( page );
				if ( result == 0 )
					break; // Need more data
				// Don't complain about missing or corrupt data yet. We'll
				// catch it at the packet output phase

				if ( result == 1 ) {
					streamState.pagein( page ); // we can ignore any errors here
					// as they'll also become apparent
					// at packetout
					while ( i < 2 ) {
						result = streamState.packetout( packet );
						if ( result == 0 ) {
							break;
						}

						if ( result == -1 ) {
							// Uh oh; data at some point was corrupted or
							// missing!
							// We can't tolerate that in a header. Die.
							throw new Exception( "Corrupt secondary header. Exiting." );
						}

						info.synthesis_headerin( comment, packet );
						i++;
					}
				}
			}

			// no harm in not checking before adding more
			index = syncState.buffer( 4096 );
			buffer = syncState.data;
			bytes = in.read( buffer, index, 4096 );

			// NOTE: This is a bugfix. read will return -1 which will mess up
			// syncState.
			if ( bytes < 0 ) {
				bytes = 0;
			}

			if ( bytes == 0 && i < 2 ) {
				throw new Exception( "End of file before finding all Vorbis headers!" );
			}

			syncState.wrote( bytes );
		}

		convsize = 4096 / info.channels;

		// OK, got and parsed all three headers. Initialize the Vorbis
		// packet->PCM decoder.
		dspState.synthesis_init( info ); // central decode state
		block.init( dspState ); // local state for most of the decode
		// so multiple block decodes can
		// proceed in parallel. We could init
		// multiple vorbis_block structures
		// for vd here
		
	}
	/**
	 * Decodes a packet.
	 * 
	 * @param packet
	 *            the packate to decode
	 * @return (unknown implementation detail).
	 * 
	 *         pvg: I did not take the time to determine what exactly is
	 *         returned here, since it's an implementation detail (by the
	 *         original author) that seems to work fine.
	 */
	private int decodePacket ( Packet packet ) {
		// check the endianes of the computer.
		final boolean bigEndian = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;

		if ( block.synthesis( packet ) == 0 ) {
			// test for success!
			dspState.synthesis_blockin( block );
		}

		// **pcm is a multichannel float vector. In stereo, for
		// example, pcm[0] is left, and pcm[1] is right. samples is
		// the size of each channel. Convert the float values
		// (-1.<=range<=1.) to whatever PCM format and write it out
		int convOff = 0;
		int samples;
		while ( (samples = dspState.synthesis_pcmout( _pcm, _index )) > 0 ) {
			// System.out.println("while() 4");
			float[][] pcm = _pcm[0];
			int bout = (samples < convsize ? samples : convsize);

			// convert floats to 16 bit signed ints (host order) and interleave
			for ( int i = 0; i < info.channels; i++ ) {
				int ptr = (i << 1) + convOff;

				int mono = _index[i];

				for ( int j = 0; j < bout; j++ ) {
					int val = (int) (pcm[i][mono + j] * 32767.);

					// might as well guard against clipping
					val = Math.max( -32768, Math.min( 32767, val ) );
					val |= (val < 0 ? 0x8000 : 0);

					convbuffer[ptr + 0] = (byte) (bigEndian ? val >>> 8 : val);
					convbuffer[ptr + 1] = (byte) (bigEndian ? val : val >>> 8);

					ptr += (info.channels) << 1;
				}
			}

			convOff += 2 * info.channels * bout;

			// Tell orbis how many samples were consumed
			dspState.synthesis_read( bout );
		}

		return convOff;
	}

	/**
	 * Decodes the next packet.
	 * 
	 * @return bytes read into convbuffer of -1 if end of file
	 * @throws IOException
	 */
	private int lazyDecodePacket () throws IOException {
		int result = getNextPacket( packet );
		if ( result == -1 ) {
			return -1;
		}

		// we have a packet. Decode it
		return decodePacket( packet );
	}

	/**
	 * @param packet
	 *            where to put the packet.
	 * @throws IOException
	 * @return 0 or -1
	 */
	private int getNextPacket ( Packet packet ) throws IOException {
		// get next packet.
		boolean fetchedPacket = false;
		while ( !eos && !fetchedPacket ) {
			int result1 = streamState.packetout( packet );
			if ( result1 == 0 ) {
				// no more packets in page. Fetch new page.
				int result2 = syncState.pageout( page );

				// return if we have reaced end of file.
				if ( (result2 == 0) && (page.eos() != 0) ) {
					return -1;
				}

				if ( result2 == 0 ) {
					// need more data fetching page..
					fetchData();
				} else if ( result2 == -1 ) {
					// throw new Exception("syncState.pageout(page) result ==
					// -1");
					System.out.println( "syncState.pageout(page) result == -1" );
					return -1;
				} else {
					// Commented out the assignment statement, since result3 as
					// never
					// used. [pvg]
					// int result3 =
					streamState.pagein( page );
				}
			} else if ( result1 == -1 ) {
				// throw new Exception("streamState.packetout(packet) result ==
				// -1");
				System.out.println( "streamState.packetout(packet) result == -1" );
				return -1;
			} else {
				fetchedPacket = true;
			}
		}

		return 0;
	}

	/**
	 * Copies data from input stream to syncState.
	 * 
	 * @throws IOException
	 */
	private void fetchData () throws IOException {
		if ( !eos ) {
			// copy 4096 bytes from compressed stream to syncState.
			int index = syncState.buffer( 4096 );
			int bytes = in.read( syncState.data, index, 4096 );
			syncState.wrote( bytes );
			if ( bytes == 0 ) {
				eos = true;
			}
		}
	}

	/**
	 * Gets information on the ogg.
	 * 
	 * @return information on the ogg
	 */
	@Override
	public String toString () {
		String s = "";
		s = s + "version         " + info.version + "\n";
		s = s + "channels        " + info.channels + "\n";
		s = s + "rate (hz)       " + info.rate;
		return s;
	}

	public int getChannels () {
		return info.channels;
	}
}