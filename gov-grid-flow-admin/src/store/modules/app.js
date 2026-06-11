import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebar: {
      opened: true
    }
  }),
  getters: {
    sidebar: state => state.sidebar
  },
  actions: {
    toggleSidebar() {
      this.sidebar.opened = !this.sidebar.opened
    }
  }
})
