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

package net.calm.slidej.io;

import net.imglib2.cache.img.DiskCachedCellImgOptions;

import java.nio.file.Paths;

public class DiskCacheOptions {
    private DiskCachedCellImgOptions options;

    public DiskCacheOptions() {
        this.options = DiskCachedCellImgOptions.options();
        setDefaults();
    }

    private void setDefaults() {
        options = options.dirtyAccesses(false);
        options = options.initializeCellsAsDirty(false);
        options = options.volatileAccesses(false);
        options = options.cacheType(DiskCachedCellImgOptions.CacheType.BOUNDED);
        options = options.cellDimensions(100);
        options=options.maxCacheSize(10000);
        options = options.cacheDirectory(Paths.get("E:/"));
        options = options.numIoThreads(Runtime.getRuntime().availableProcessors());
    }

    public DiskCachedCellImgOptions getOptions() {
        return options;
    }
}
