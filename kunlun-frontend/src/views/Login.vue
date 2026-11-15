<template>
  <div class="login-wrapper">
    <div class="login-bg"></div>
    <div class="login-overlay">
      <div class="login-card">
        <div class="login-header">
          <div class="login-logo">⛽</div>
          <h1 class="login-title">昆仑智联</h1>
          <p class="login-subtitle">智慧油品配送管理平台</p>
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="0"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              class="login-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
              class="login-input"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await axios.post('/api/auth/login', {
      username: form.username,
      password: form.password,
    })
    const { token, role } = res.data
    localStorage.setItem('token', token)
    localStorage.setItem('role', role)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (err: any) {
    const msg = err.response?.data?.error || '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(0, 82, 147, 0.85) 0%, rgba(0, 52, 94, 0.9) 100%),
    url('/assets/bg.jpg') center / cover no-repeat;
  filter: brightness(0.7);
}

.login-overlay {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.login-card {
  width: 420px;
  padding: 48px 40px 36px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(16px);
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.25);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  font-size: 48px;
  margin-bottom: 8px;
}

.login-title {
  margin: 0;
  font-size: 28px;
  font-weight: 800;
  color: #005293;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', sans-serif;
  letter-spacing: 4px;
}

.login-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #909399;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.login-form {
  margin-top: 8px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 24px;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: box-shadow 0.2s;
}

.login-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #409eff inset;
}

.login-input :deep(.el-input__inner) {
  font-size: 15px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  height: 44px;
  line-height: 44px;
}

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  letter-spacing: 8px;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: linear-gradient(135deg, #005293, #0073c6);
  border: none;
}

.login-btn:hover {
  background: linear-gradient(135deg, #003d6e, #005ea3);
}
</style>
