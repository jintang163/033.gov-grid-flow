export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/report/index',
    'pages/offline/index',
    'pages/mine/index',
    'pages/detail/index',
    'pages/map-cache/index',
    'pages/sync-log/index'
  ],
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#165dff',
    navigationBarTitleText: '政务网格',
    navigationBarTextStyle: 'white',
    backgroundColor: '#f5f6f7'
  },
  tabBar: {
    color: '#86909C',
    selectedColor: '#165dff',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      {
        pagePath: 'pages/home/index',
        text: '首页'
      },
      {
        pagePath: 'pages/report/index',
        text: '上报'
      },
      {
        pagePath: 'pages/offline/index',
        text: '本地'
      },
      {
        pagePath: 'pages/mine/index',
        text: '我的'
      }
    ]
  }
})
