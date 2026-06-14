/**
 * 前端图片水印工具
 * 使用Canvas API在图片上传前自动添加水印
 */

export interface WatermarkOptions {
  reportTime: string
  reporterName: string
  eventNo?: string
  fontSize?: number
  fontColor?: string
  opacity?: number
}

export interface WatermarkResult {
  file: File
  originalMd5: string
}

export const addImageWatermark = (file: File, options: WatermarkOptions): Promise<WatermarkResult> => {
  return new Promise((resolve, reject) => {
    const { reportTime, reporterName, eventNo, fontSize = 14, fontColor = '#FF0000', opacity = 0.3 } = options

    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')!

        const width = img.width
        const height = img.height

        canvas.width = width
        canvas.height = height

        ctx.drawImage(img, 0, 0, width, height)

        ctx.font = `${fontSize}px SimHei, sans-serif`
        ctx.fillStyle = fontColor
        ctx.globalAlpha = opacity
        ctx.textBaseline = 'top'

        const margin = 20
        const lineHeight = fontSize + 6

        const line1 = `上报时间：${reportTime}`
        const line2 = `网格员：${reporterName}`
        const line3 = eventNo ? `事件编号：${eventNo}` : ''

        let y = height - margin - (line3 ? 2 : 1) * lineHeight

        ctx.fillText(line1, margin, y)
        ctx.fillText(line2, margin, y + lineHeight)
        if (line3) {
          ctx.fillText(line3, margin, y + 2 * lineHeight)
        }

        const tileWidth = 200
        const tileHeight = 80
        const tileRows = Math.ceil(height / tileHeight) + 1
        const tileCols = Math.ceil(width / tileWidth) + 1

        ctx.save()
        ctx.globalAlpha = opacity * 0.5
        ctx.translate(width / 2, height / 2)
        ctx.rotate((-30 * Math.PI) / 180)

        const tileText = eventNo || reporterName

        for (let row = 0; row < tileRows; row++) {
          for (let col = 0; col < tileCols; col++) {
            const x = col * tileWidth - width / 2
            const ty = row * tileHeight - height / 2
            ctx.fillText(tileText, x, ty)
          }
        }

        ctx.restore()

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error('水印添加失败'))
              return
            }

            const watermarkedFile = new File([blob], file.name, {
              type: file.type || 'image/jpeg',
              lastModified: Date.now()
            })

            calculateMD5(file).then(originalMd5 => {
              resolve({
                file: watermarkedFile,
                originalMd5
              })
            }).catch(reject)
          },
          file.type || 'image/jpeg',
          0.8
        )
      }

      img.onerror = () => {
        reject(new Error('图片加载失败'))
      }

      img.src = e.target?.result as string
    }

    reader.onerror = () => {
      reject(new Error('文件读取失败'))
    }

    reader.readAsDataURL(file)
  })
}

export const calculateMD5 = async (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = async (e) => {
      try {
        const arrayBuffer = e.target?.result as ArrayBuffer
        const hashBuffer = await crypto.subtle.digest('MD5', arrayBuffer)
        const hashArray = Array.from(new Uint8Array(hashBuffer))
        const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
        resolve(hashHex)
      } catch {
        const sparkMd5 = await import('spark-md5')
        const hash = sparkMd5.ArrayBuffer.hash(e.target?.result as ArrayBuffer)
        resolve(hash)
      }
    }
    reader.onerror = () => reject(new Error('MD5计算失败'))
    reader.readAsArrayBuffer(file)
  })
}

export const addVideoWatermark = async (file: File, options: WatermarkOptions): Promise<WatermarkResult> => {
  const originalMd5 = await calculateMD5(file)
  return {
    file,
    originalMd5
  }
}

export default {
  addImageWatermark,
  addVideoWatermark,
  calculateMD5
}
