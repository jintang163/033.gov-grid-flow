import React, { useCallback } from 'react';
import { View, Image, Text } from '@tarojs/components';
import Taro, { chooseImage, getImageInfo } from '@tarojs/taro';
import classnames from 'classnames';
import styles from './index.module.scss';

export interface ImageItem {
  id: string;
  path: string;
  size: number;
  width?: number;
  height?: number;
}

interface ImageUploaderProps {
  images: ImageItem[];
  onChange: (images: ImageItem[]) => void;
  maxCount?: number;
  disabled?: boolean;
}

const ImageUploader: React.FC<ImageUploaderProps> = ({
  images,
  onChange,
  maxCount = 9,
  disabled = false
}) => {
  const handleChooseImage = useCallback(async () => {
    if (disabled) return;
    if (images.length >= maxCount) {
      Taro.showToast({
        title: `最多只能上传${maxCount}张图片`,
        icon: 'none'
      });
      return;
    }

    try {
      const res = await chooseImage({
        count: maxCount - images.length,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera']
      });

      const newImages: ImageItem[] = [];

      for (const tempFilePath of res.tempFilePaths) {
        try {
          const fileInfo = await Taro.getFileInfo({ filePath: tempFilePath });
          const imgInfo = await getImageInfo({ src: tempFilePath });

          newImages.push({
            id: `img_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
            path: tempFilePath,
            size: fileInfo.size || 0,
            width: imgInfo.width,
            height: imgInfo.height
          });
        } catch (error) {
          console.error('[ImageUploader] 获取图片信息失败:', error);
          newImages.push({
            id: `img_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
            path: tempFilePath,
            size: 0
          });
        }
      }

      onChange([...images, ...newImages]);
      console.log('[ImageUploader] 已选择图片:', newImages.length);
    } catch (error) {
      console.error('[ImageUploader] 选择图片失败:', error);
      if ((error as any).errMsg !== 'chooseImage:fail cancel') {
        Taro.showToast({
          title: '选择图片失败',
          icon: 'none'
        });
      }
    }
  }, [images, maxCount, disabled, onChange]);

  const handleRemoveImage = useCallback((index: number) => {
    if (disabled) return;
    Taro.showModal({
      title: '确认删除',
      content: '确定要删除这张图片吗？',
      success: (res) => {
        if (res.confirm) {
          const newImages = [...images];
          newImages.splice(index, 1);
          onChange(newImages);
        }
      }
    });
  }, [images, disabled, onChange]);

  const handlePreviewImage = useCallback((index: number) => {
    const urls = images.map(img => img.path);
    Taro.previewImage({
      current: urls[index],
      urls
    });
  }, [images]);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  return (
    <View className={styles.imageUploader}>
      <View className={styles.imageList}>
        {images.map((image, index) => (
          <View key={image.id} className={styles.imageItem}>
            <Image
              className={styles.image}
              src={image.path}
              mode="aspectFill"
              onClick={() => handlePreviewImage(index)}
            />
            {!disabled && (
              <View
                className={styles.removeBtn}
                onClick={() => handleRemoveImage(index)}
              >
                <Text className={styles.removeIcon}>×</Text>
              </View>
            )}
            <View className={styles.imageInfo}>
              <Text className={styles.imageSize}>{formatFileSize(image.size)}</Text>
            </View>
          </View>
        ))}

        {images.length < maxCount && !disabled && (
          <View
            className={classnames(styles.imageItem, styles.addBtn)}
            onClick={handleChooseImage}
          >
            <Text className={styles.addIcon}>+</Text>
            <Text className={styles.addText}>添加图片</Text>
          </View>
        )}
      </View>

      <View className={styles.uploaderTip}>
        <Text className={styles.tipText}>
          已选 {images.length}/{maxCount} 张图片（支持拍照和相册选择）
        </Text>
      </View>
    </View>
  );
};

export default ImageUploader;
