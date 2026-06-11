<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-title">
        <div class="logo-icon">
          <el-icon :size="48"><OfficeBuilding /></el-icon>
        </div>
        <h2>政务网格流程管理系统</h2>
        <p>Government Grid Flow Management System</p>
      </div>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="code">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.code"
              placeholder="请输入验证码"
              :prefix-icon="Key"
              style="flex: 1"
              maxlength="4"
            />
            <div class="captcha-img" @click="refreshCaptcha">
              <canvas ref="captchaCanvas" width="110" height="40"></canvas>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            style="width: 100%; font-size: 16px; padding: 12px 0"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>© 2024 政务网格流程管理系统</span>
      </div>
    </div>
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref()
const captchaCanvas = ref()
const loading = ref(false)
const captchaCode = ref('')

const loginForm = reactive({
  username: 'admin',
  password: '123456',
  code: '',
  remember: true
})

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为4位', trigger: 'blur' }
  ]
}

function generateCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  captchaCode.value = code
  drawCaptcha(code)
}

function drawCaptcha(code) {
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = '#f5f7fa'
  ctx.fillRect(0, 0, 110, 40)

  for (let i = 0; i < 5; i++) {
    ctx.strokeStyle = randomColor(150, 230)
    ctx.beginPath()
    ctx.moveTo(Math.random() * 110, Math.random() * 40)
    ctx.lineTo(Math.random() * 110, Math.random() * 40)
    ctx.stroke()
  }

  for (let i = 0; i < 40; i++) {
    ctx.fillStyle = randomColor(100, 200)
    ctx.beginPath()
    ctx.arc(Math.random() * 110, Math.random() * 40, 1, 0, 2 * Math.PI)
    ctx.fill()
  }

  const colors = ['#667eea', '#764ba2', '#f093fb', '#4facfe', '#43e97b', '#fa709a']
  for (let i = 0; i < code.length; i++) {
    ctx.font = 'bold 22px Arial'
    ctx.fillStyle = colors[Math.floor(Math.random() * colors.length)]
    ctx.save()
    ctx.translate(18 + i * 22, 26)
    ctx.rotate((Math.random() - 0.5) * 0.4)
    ctx.fillText(code[i], 0, 0)
    ctx.restore()
  }
}

function randomColor(min, max) {
  const r = Math.floor(Math.random() * (max - min) + min)
  const g = Math.floor(Math.random() * (max - min) + min)
  const b = Math.floor(Math.random() * (max - min) + min)
  return `rgb(${r},${g},${b})`
}

function refreshCaptcha() {
  generateCaptcha()
  loginForm.code = ''
}

async function handleLogin() {
  try {
    await loginFormRef.value.validate()
    if (loginForm.code.toLowerCase() !== captchaCode.value.toLowerCase()) {
      ElMessage.error('验证码错误')
      refreshCaptcha()
      return
    }
    loading.value = true
    try {
      await userStore.login(loginForm)
      await userStore.getUserInfoAction()
      ElMessage.success('登录成功')
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } catch (error) {
      ElMessage.error(error.message || '登录失败')
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  } catch (e) {
  }
}

onMounted(() => {
  generateCaptcha()
})
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;

  .circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
    animation: float 6s ease-in-out infinite;
  }

  .circle-1 {
    width: 300px;
    height: 300px;
    top: -100px;
    left: -100px;
  }

  .circle-2 {
    width: 400px;
    height: 400px;
    bottom: -150px;
    right: -150px;
    animation-delay: 2s;
  }

  .circle-3 {
    width: 200px;
    height: 200px;
    top: 50%;
    left: 10%;
    animation-delay: 4s;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

.login-box {
  width: 420px;
  padding: 40px;
  background-color: rgba(255, 255, 255, 0.98);
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  position: relative;
  z-index: 1;

  .login-title {
    text-align: center;
    margin-bottom: 30px;

    .logo-icon {
      width: 80px;
      height: 80px;
      margin: 0 auto 16px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    h2 {
      margin: 0 0 8px 0;
      color: #303133;
      font-size: 22px;
      font-weight: 600;
    }

    p {
      margin: 0;
      color: #909399;
      font-size: 13px;
    }
  }

  .login-form {
    .el-form-item {
      margin-bottom: 20px;
    }

    .captcha-row {
      display: flex;
      gap: 10px;
      align-items: center;

      .captcha-img {
        width: 110px;
        height: 40px;
        cursor: pointer;
        border-radius: 4px;
        overflow: hidden;
        border: 1px solid #dcdfe6;
        transition: border-color 0.2s;

        &:hover {
          border-color: #667eea;
        }

        canvas {
          display: block;
        }
      }
    }
  }

  .login-footer {
    text-align: center;
    margin-top: 20px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    color: #909399;
    font-size: 12px;
  }
}
</style>
