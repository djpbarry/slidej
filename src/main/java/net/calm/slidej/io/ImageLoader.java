/*
 * Copyright (c)  2020, David J. Barry
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * 
 */
package net.calm.slidej.io;

import io.scif.ImageMetadata;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;
import io.scif.services.DatasetIOService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

import java.io.File;
import java.util.ArrayList;

/**
 * 
 * @author david.barry@crick.ac.uk
 *
 */
public class ImageLoader <T extends NumericType<T> & NativeType<T>> {

	private ImageMetadata meta;

	public ImageLoader() {

	}

	public Img<T> load(File file, int series) {
		SCIFIOConfig config = new SCIFIOConfig();
		config.imgOpenerSetIndex(series);

		SCIFIOImgPlus<?> sciImg = new ImgOpener().openImgs(file.getAbsolutePath(), config).get(0);
		this.meta = sciImg.getImageMetadata();

		return (Img<T>)sciImg;
	}

	public RandomAccessibleInterval<T> loadAndConcatenate(File dir, int concatAxis){
		File[] inputs = dir.listFiles();
		ArrayList<RandomAccessibleInterval<T>> imgs = new ArrayList<>();
		DatasetIOService io = (new SCIFIO()).datasetIO();
		for(File f:inputs){
			if(io.canOpen(f.getAbsolutePath())){
				imgs.add(Views.addDimension(load(f, 0), 0, 0));
			}
		}
		RandomAccessibleInterval<T> concatImgs = Views.concatenate(imgs.get(0).numDimensions() - 1, imgs);
		return Views.moveAxis(concatImgs, concatImgs.numDimensions() - 1, concatAxis);
	}

	public ImageMetadata getMeta() {
		return meta;
	}
}