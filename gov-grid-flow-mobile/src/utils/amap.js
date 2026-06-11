import AMapLoader from 'amap-js-api-loader'

let aMapInstance = null

export const initAMap = async () => {
  if (aMapInstance) {
    return aMapInstance
  }
  aMapInstance = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.Geolocation', 'AMap.Geocoder', 'AMap.PlaceSearch', 'AMap.ToolBar']
  })
  return aMapInstance
}

export const getCurrentLocation = async () => {
  const AMap = await initAMap()
  return new Promise((resolve, reject) => {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      zoomToAccuracy: true
    })
    geolocation.getCurrentPosition((status, result) => {
      if (status === 'complete') {
        resolve(result)
      } else {
        reject(result)
      }
    })
  })
}

export const getAddressByLngLat = async (lng, lat) => {
  const AMap = await initAMap()
  return new Promise((resolve, reject) => {
    const geocoder = new AMap.Geocoder()
    geocoder.getAddress([lng, lat], (status, result) => {
      if (status === 'complete' && result.info === 'OK') {
        resolve(result.regeocode)
      } else {
        reject(result)
      }
    })
  })
}
