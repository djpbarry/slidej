package slidej.io;

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
