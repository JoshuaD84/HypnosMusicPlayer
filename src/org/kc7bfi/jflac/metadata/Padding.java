package org.kc7bfi.jflac.metadata;

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

import java.io.IOException;

import org.kc7bfi.jflac.io.BitInputStream;

/**
 * Padding Metadata block.
 * @author kc7bfi
 */
public class Padding extends Metadata {
    private int length;

    /**
     * The constructor.
     * @param is                The InputBitStream
     * @param length            Length of the record
     * @param isLast            True if this is the last Metadata block in the chain
     * @throws IOException      Thrown if error reading from InputBitStream
     */
    public Padding(BitInputStream is, int length, boolean isLast) throws IOException {
        super(isLast);
        this.length = length;
        is.readByteBlockAlignedNoCRC(null, length);
    }
    
    /**
     * Convert to string.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "Padding (Length=" + length + ")";
    }
}
