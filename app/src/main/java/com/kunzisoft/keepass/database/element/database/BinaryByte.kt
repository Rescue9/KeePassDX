/*
 * Copyright 2018 Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePassDX.
 *
 *  KeePassDX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.kunzisoft.keepass.database.element.database

import android.os.Parcel
import android.os.Parcelable
import com.kunzisoft.keepass.database.element.Database
import com.kunzisoft.keepass.stream.readAllBytes
import java.io.*
import java.util.zip.GZIPOutputStream

class BinaryByte : BinaryData {

    private var mDataByte: ByteArray = ByteArray(0)

    /**
     * Empty protected binary
     */
    constructor() : super()

    constructor(byteArray: ByteArray,
                compressed: Boolean = false,
                protected: Boolean = false) : super(compressed, protected) {
        this.mDataByte = byteArray
    }

    constructor(parcel: Parcel) : super(parcel) {
        val byteArray = ByteArray(parcel.readInt())
        parcel.readByteArray(byteArray)
        mDataByte = byteArray
    }

    @Throws(IOException::class)
    override fun getInputDataStream(cipherKey: Database.LoadedKey): InputStream {
        return ByteArrayInputStream(mDataByte)
    }

    @Throws(IOException::class)
    override fun getOutputDataStream(cipherKey: Database.LoadedKey): OutputStream {
        return ByteOutputStream()
    }

    @Throws(IOException::class)
    override fun compress(cipherKey: Database.LoadedKey) {
        if (!isCompressed) {
            GZIPOutputStream(getOutputDataStream(cipherKey)).use { outputStream ->
                getInputDataStream(cipherKey).use { inputStream ->
                    inputStream.readAllBytes { buffer ->
                        outputStream.write(buffer)
                    }
                }
                isCompressed = true
            }
        }
    }

    @Throws(IOException::class)
    override fun decompress(cipherKey: Database.LoadedKey) {
        if (isCompressed) {
            getUnGzipInputDataStream(cipherKey).use { inputStream ->
                getOutputDataStream(cipherKey).use { outputStream ->
                    inputStream.readAllBytes { buffer ->
                        outputStream.write(buffer)
                    }
                }
                isCompressed = false
            }
        }
    }

    @Throws(IOException::class)
    override fun clear() {
        mDataByte = ByteArray(0)
    }

    override fun dataExists(): Boolean {
        return mDataByte.isNotEmpty()
    }

    override fun getSize(): Long {
        return mDataByte.size.toLong()
    }

    /**
     * Hash of the raw encrypted file in temp folder, only to compare binary data
     */
    override fun binaryHash(): Int {
        return if (dataExists())
            mDataByte.contentHashCode()
        else
            0
    }

    override fun toString(): String {
        return mDataByte.toString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeInt(mDataByte.size)
        dest.writeByteArray(mDataByte)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BinaryByte) return false
        if (!super.equals(other)) return false

        if (!mDataByte.contentEquals(other.mDataByte)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + mDataByte.contentHashCode()
        return result
    }

    /**
     * Custom OutputStream to calculate the size and hash of binary file
     */
    private inner class ByteOutputStream : ByteArrayOutputStream() {
        override fun close() {
            mDataByte = this.toByteArray()
            super.close()
        }
    }

    companion object {

        private val TAG = BinaryByte::class.java.name
        const val MAX_BINARY_BYTES = 10240

        @JvmField
        val CREATOR: Parcelable.Creator<BinaryByte> = object : Parcelable.Creator<BinaryByte> {
            override fun createFromParcel(parcel: Parcel): BinaryByte {
                return BinaryByte(parcel)
            }

            override fun newArray(size: Int): Array<BinaryByte?> {
                return arrayOfNulls(size)
            }
        }
    }

}