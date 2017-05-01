package org.kc7bfi.jflac.sound.spi;

/**
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

import javax.sound.sampled.AudioFileFormat;

/**
 * FileFormatTypes used by the FLAC audio decoder.
 */
public class FlacFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an FLAC file.
     */
    public static final AudioFileFormat.Type FLAC = new FlacFileFormatType("FLAC", "flac");

    /**
     * Constructs a file type.
     * 
     * @param name -
     *            the name of the Flac File Format.
     * @param extension -
     *            the file extension for this Flac File Format.
     */
    public FlacFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
