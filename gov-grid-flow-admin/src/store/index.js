import { createPinia } from 'pinia'
import { useUserStore } from './modules/user'
import { useAppStore } from './modules/app'

const pinia = createPinia()

export default pinia

export { useUserStore, useAppStore }
