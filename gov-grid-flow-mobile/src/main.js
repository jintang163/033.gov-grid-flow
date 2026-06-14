import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import {
  Button,
  Field,
  CellGroup,
  Form,
  NavBar,
  Tabbar,
  TabbarItem,
  Icon,
  List,
  Cell,
  PullRefresh,
  Toast,
  Dialog,
  Notify,
  Loading,
  Image as VanImage,
  Grid,
  GridItem,
  Card,
  Tag,
  Popup,
  Picker,
  DatePicker,
  Uploader,
  ActionSheet,
  Steps,
  Step,
  Collapse,
  CollapseItem,
  Divider,
  Empty,
  NoticeBar,
  Progress,
  Swipe,
  SwipeItem,
  Search,
  DropdownMenu,
  DropdownItem
} from 'vant'
import 'vant/lib/index.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.use(Button)
app.use(Field)
app.use(CellGroup)
app.use(Form)
app.use(NavBar)
app.use(Tabbar)
app.use(TabbarItem)
app.use(Icon)
app.use(List)
app.use(Cell)
app.use(PullRefresh)
app.use(Toast)
app.use(Dialog)
app.use(Notify)
app.use(Loading)
app.use(VanImage)
app.use(Grid)
app.use(GridItem)
app.use(Card)
app.use(Tag)
app.use(Popup)
app.use(Picker)
app.use(DatePicker)
app.use(Uploader)
app.use(ActionSheet)
app.use(Steps)
app.use(Step)
app.use(Collapse)
app.use(CollapseItem)
app.use(Divider)
app.use(Empty)
app.use(NoticeBar)
app.use(Progress)
app.use(Swipe)
app.use(SwipeItem)
app.use(Search)
app.use(DropdownMenu)
app.use(DropdownItem)
app.use(Badge)
app.use(Slider)
app.use(Switch)

app.mount('#app')
