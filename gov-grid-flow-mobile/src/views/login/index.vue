<template>
  <div class="login-container">
    <div class="login-header">
      <div class="logo-wrap">
        <van-icon name="orders-o" size="64" color="#fff" />
      </div>
      <h2 class="login-title">政务网格流转系统</h2>
      <p class="login-subtitle">基层治理 · 高效便民</p>
    </div>

    <div class="login-form-wrap">
      <van-tabs v-model:active="loginType" class="login-tabs" line-width="40px">
        <van-tab title="账号密码登录" name="password" />
        <van-tab title="验证码登录" name="code" />
      </van-tabs>

      <van-form ref="formRef" @submit="onSubmit" class="login-form">
        <van-cell-group v-if="loginType === 'password'" inset>
          <van-field
            v-model="form.username"
            name="username"
            label="账号"
            placeholder="请输入手机号/账号"
            :rules="[
              { required: true, message: '请填写账号' },
              { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '账号格式不正确' }
            ]"
          >
            <template #left-icon>
              <van-icon name="user-o" size="18" />
            </template>
          </van-field>
          <van-field
            v-model="form.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请填写密码' }]"
          >
            <template #left-icon>
              <van-icon name="lock" size="18" />
            </template>
          </van-field>
          <van-field
            v-model="form.code"
            name="code"
            label="验证码"
            placeholder="请输入图形验证码"
            :rules="[{ required: true, message: '请填写验证码' }]"
          >
            <template #left-icon>
              <van-icon name="shield-o" size="18" />
            </template>
            <template #button>
              <div class="captcha-btn" @click="refreshCaptcha">
                <img :src="captchaUrl" alt="验证码" class="captcha-img" />
              </div>
            </template>
          </van-field>
        </van-cell-group>

        <van-cell-group v-if="loginType === 'code'" inset>
          <van-field
            v-model="form.phone"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            :rules="[
              { required: true, message: '请填写手机号' },
              { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
            ]"
          >
            <template #left-icon>
              <van-icon name="phone-o" size="18" />
            </template>
          </van-field>
          <van-field
            v-model="form.smsCode"
            name="smsCode"
            label="验证码"
            placeholder="请输入短信验证码"
            :rules="[{ required: true, message: '请填写短信验证码' }]"
          >
            <template #left-icon>
              <van-icon name="chat-o" size="18" />
            </template>
            <template #button>
              <van-button
                size="small"
                type="primary"
                plain
                :disabled="counting"
                @click="sendSmsCode"
              >
                {{ counting ? `${countdown}s后重发` : '获取验证码' }}
              </van-button>
            </template>
          </van-field>
        </van-cell-group>

        <div class="login-actions">
          <van-cell-group inset>
            <van-field name="remember" v-model="form.remember">
              <template #input>
                <div class="remember-wrap">
                  <van-checkbox v-model="form.remember" shape="square">记住密码</van-checkbox>
                  <span class="forgot-link" @click="onForgotPassword">忘记密码?</span>
                </div>
              </template>
            </van-field>
          </van-cell-group>
        </div>

        <div class="submit-wrap">
          <van-button round block type="primary" size="large" native-type="submit" :loading="loading">
            登 录
          </van-button>
        </div>
      </van-form>

      <div class="login-footer">
        <p class="agreement-text">
          登录即表示同意
          <span class="link" @click="showAgreement('user')">《用户协议》</span>
          和
          <span class="link" @click="showAgreement('privacy')">《隐私政策》</span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { useUserStore } from '@/store'
import { sendCode } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const loginType = ref('password')
const counting = ref(false)
const countdown = ref(60)
const captchaUrl = ref('')

const form = reactive({
  username: '',
  password: '',
  phone: '',
  code: '',
  smsCode: '',
  remember: false
})

const refreshCaptcha = () => {
  captchaUrl.value = `/captcha?time=${Date.now()}`
}

const sendSmsCode = async () => {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    showToast('请输入正确的手机号')
    return
  }
  try {
    await sendCode(form.phone)
    showToast('验证码发送成功')
    counting.value = true
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        counting.value = false
      }
    }, 1000)
  } catch (e) {
    console.error(e)
  }
}

const onSubmit = async (values) => {
  loading.value = true
  try {
    const loginData = loginType.value === 'password'
      ? { username: values.username, password: values.password, code: values.code }
      : { username: values.phone, password: values.smsCode, code: values.smsCode }

    await userStore.login(loginData)
    showToast('登录成功')
    if (form.remember) {
      localStorage.setItem('rememberAccount', JSON.stringify({
        username: form.username || form.phone,
        password: form.password
      }))
    } else {
      localStorage.removeItem('rememberAccount')
    }
    router.replace('/home')
  } catch (e) {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const onForgotPassword = () => {
  showDialog({
    title: '忘记密码',
    message: '请联系管理员重置密码',
    confirmButtonText: '我知道了'
  })
}

const showAgreement = (type) => {
  const title = type === 'user' ? '用户协议' : '隐私政策'
  const content = type === 'user'
    ? '欢迎使用政务网格流转系统。本协议是您与系统运营方之间关于使用本系统服务所订立的协议。请您仔细阅读本协议的全部内容。'
    : '我们非常重视您的个人信息和隐私保护。我们将按照法律法规要求，采取相应安全保护措施，尽力保护您的个人信息安全可控。'
  showDialog({
    title,
    message: content,
    confirmButtonText: '我知道了'
  })
}

onMounted(() => {
  refreshCaptcha()
  const remembered = localStorage.getItem('rememberAccount')
  if (remembered) {
    try {
      const data = JSON.parse(remembered)
      form.username = data.username || ''
      form.password = data.password || ''
      form.remember = true
    } catch (e) {
      console.error(e)
    }
  }
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #1989fa 0%, #5fb7ff 30%, #f7f8fa 60%);
  padding: 0 0 40px;
}

.login-header {
  text-align: center;
  padding: 60px 0 40px;
  color: #fff;
}

.logo-wrap {
  width: 96px;
  height: 96px;
  margin: 0 auto 20px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 13px;
  opacity: 0.9;
  margin: 0;
}

.login-form-wrap {
  margin: 0 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 0 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.login-tabs {
  padding: 0 8px;
}

.login-form {
  margin-top: 16px;
}

.captcha-btn {
  cursor: pointer;
  padding: 4px 8px;
}

.captcha-img {
  height: 32px;
  border-radius: 4px;
  display: block;
}

.login-actions {
  margin-top: 12px;
}

.remember-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.forgot-link {
  font-size: 13px;
  color: #1989fa;
}

.submit-wrap {
  margin: 24px 16px 0;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding: 0 24px;
}

.agreement-text {
  font-size: 12px;
  color: #969799;
  line-height: 1.6;
  margin: 0;
}

.link {
  color: #1989fa;
}
</style>
