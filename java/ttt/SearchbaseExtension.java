// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.

/*
 * Created on 06.04.2006
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 */
package ttt;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SearchbaseExtension {

    // write searchbase extension
    static public void writeSearchbaseExtension(DataOutputStream out, Index index) throws IOException {
        if (index != null && index.getSearchbaseFormat() == Index.XML_SEARCHBASE && index.size() > 0) {
            // buffer output to determine its length
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream buffer = new DataOutputStream(byteArrayOutputStream);

            // header
            buffer.writeByte(Constants.EXTENSION_SEARCHBASE_TABLE_WITH_COORDINATES);

            // number of index entries
            buffer.writeShort(index.size());

            // ratio
            // NOTE: this is Omnipage XML Document specific, where coordinates differ from input screenshot.
            // However, it seems to be persistent for all pages (will fail, if not)
            // TODO: think about this
            double ratio = 0;
            // TODO: ugly hack
            // NOTE: ratio of 1 might be returned for pages without searchable text
            for (int i = 0; ((ratio == 0) || (ratio == 1)) && (i < index.size()); i++)
                ratio = index.get(i).getRatio();

            if (ratio == 0)
                System.out.println("Bad ratio (zero) written. This should never habben!!!!!!!! (Now is never;-)");

            buffer.writeDouble(ratio);

            // write searchbase for each index
            for (int i = 0; i < index.size(); i++)
                index.get(i).writeSearchbase(buffer);

            // force write
            buffer.flush();

            // TODO: only one write would be more secure. however, it's unlikely that only second write fails
            // write length of extension
            out.writeInt(byteArrayOutputStream.size());
            // write extension
            out.write(byteArrayOutputStream.toByteArray());

            index.searchbaseFormatStored = Index.XML_SEARCHBASE;
        } // else no index available
    }

    // TODO: return value??

    // read searchbase extension
    static public void readSearchbaseExtension(DataInputStream in, Index index) throws IOException {
        // NOTE: assumes header tag already read

        // number of index entries
        int size = in.readShort();

        if (size != index.size())
            throw new IOException(
                    "Number of entries in SEARCHBASE EXTENSION does not match with index of recording!!!!!!!!!");

        // ratio
        // NOTE: This is Omnipage XML Document specific, where coordinates differ from input screenshot.
        // However, it seems to be persistent for all pages (will fail, if not)
        double ratio = in.readDouble();

        // write searchbase for each index
        for (int i = 0; i < index.size(); i++)
            index.get(i).readSearchbase(in, ratio);

        index.searchbaseFormat = index.searchbaseFormatStored = Index.XML_SEARCHBASE;
    }

}
