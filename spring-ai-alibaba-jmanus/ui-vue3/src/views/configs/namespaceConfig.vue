<!--
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->
<template>
  <div class="config-panel">
    <div class="panel-header">
      <h2>{{ t('config.namespaceConfig.title') }}</h2>
    </div>

    <div class="namespace-layout">
      <!-- Namespace列表 -->
      <div class="namespace-list">
        <div class="list-header">
          <h3>{{ t('config.namespaceConfig.configuredNamespaces') }}</h3>
          <span class="namespace-count">({{ namespaces.length }})</span>
        </div>

        <div class="namespaces-container" v-if="!loading">
          <div
              v-for="namespace in namespaces"
              :key="namespace.id"
              class="namespace-card"
              :class="{ active: selectedNamespace?.id === namespace.id }"
              @click="selectNamespace(namespace)"
          >
            <div class="namespace-card-header">
              <span class="namespace-name">{{ namespace.name }}</span>
              <Icon icon="carbon:chevron-right" />
            </div>
            <p class="namespace-desc">{{ namespace.description }}</p>
            <div class="namespace-code">
              <span class="namespace-tag">
                {{ namespace.code }}
              </span>
            </div>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <Icon icon="carbon:loading" class="loading-icon" />
          {{ t('common.loading') }}
        </div>

        <div v-if="!loading && namespaces.length === 0" class="empty-state">
          <Icon icon="carbon:folder" class="empty-icon" />
          <p>{{ t('config.namespaceConfig.noNamespace') }}</p>
        </div>

        <button class="add-btn" @click="showAddNamespaceModal">
          <Icon icon="carbon:add" />
          {{ t('config.namespaceConfig.createNew') }}
        </button>
      </div>

      <!-- Namespace详情 -->
      <div class="namespace-detail" v-if="selectedNamespace">
        <div class="detail-header">
          <h3>{{ selectedNamespace.name }}</h3>
          <div class="detail-actions">
            <button class="action-btn primary" @click="handleSave">
              <Icon icon="carbon:save" />
              {{ t('common.save') }}
            </button>
            <button class="action-btn danger" @click="showDeleteConfirm">
              <Icon icon="carbon:trash-can" />
              {{ t('common.delete') }}
            </button>
          </div>
        </div>

        <div class="form-item">
          <label>{{ t('config.namespaceConfig.name') }} <span class="required">*</span></label>
          <input
              type="text"
              v-model="selectedNamespace.name"
              :placeholder="t('config.namespaceConfig.namePlaceholder')"
              required
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.namespaceConfig.code') }} <span class="required">*</span></label>
          <input
              type="text"
              v-model="selectedNamespace.code"
              :placeholder="t('config.namespaceConfig.codePlaceholder')"
              required
          />
        </div>

        <div class="form-item">
          <label>{{ t('config.namespaceConfig.description') }} <span class="required">*</span></label>
          <textarea
              v-model="selectedNamespace.description"
              rows="3"
              :placeholder="t('config.namespaceConfig.descriptionPlaceholder')"
              required
          ></textarea>
        </div>

      </div>

      <!-- 空状态 -->
      <div v-else class="no-selection">
        <Icon icon="carbon:folder" class="placeholder-icon" />
        <p>{{ t('config.namespaceConfig.selectNamespaceHint') }}</p>
      </div>
    </div>

    <!-- 新建Namespace弹窗 -->
    <Modal v-model="showModal" :title="t('config.namespaceConfig.newNamespace')" @confirm="handleAddNamespace">
      <div class="modal-form">
        <div class="form-item">
          <label>{{ t('config.namespaceConfig.name') }} <span class="required">*</span></label>
          <input
              type="text"
              v-model="newNamespace.name"
              :placeholder="t('config.namespaceConfig.namePlaceholder')"
              required
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.namespaceConfig.code') }} <span class="required">*</span></label>
          <input
              type="text"
              v-model="newNamespace.code"
              :placeholder="t('config.namespaceConfig.codePlaceholder')"
              required
          />
        </div>
        <div class="form-item">
          <label>{{ t('config.namespaceConfig.description') }} <span class="required">*</span></label>
          <textarea
              v-model="newNamespace.description"
              rows="3"
              :placeholder="t('config.namespaceConfig.descriptionPlaceholder')"
              required
          ></textarea>
        </div>
      </div>
    </Modal>

    <!-- 删除确认弹窗 -->
    <Modal v-model="showDeleteModal" title="删除确认">
      <div class="delete-confirm">
        <Icon icon="carbon:warning" class="warning-icon" />
        <p>{{ t('config.namespaceConfig.deleteConfirmText') }} <strong>{{ selectedNamespace?.name }}</strong> {{ t('common.confirm') }}？</p>
        <p class="warning-text">{{ t('config.namespaceConfig.deleteWarning') }}</p>
      </div>
      <template #footer>
        <button class="cancel-btn" @click="showDeleteModal = false">{{ t('common.cancel') }}</button>
        <button class="confirm-btn danger" @click="handleDelete">{{ t('common.delete') }}</button>
      </template>
    </Modal>

    <!-- 错误提示 -->
    <div v-if="error" class="error-toast" @click="error = ''">
      <Icon icon="carbon:error" />
      {{ error }}
    </div>

    <!-- 成功提示 -->
    <div v-if="success" class="success-toast" @click="success = ''">
      <Icon icon="carbon:checkmark" />
      {{ success }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Icon } from '@iconify/vue'
import { useI18n } from 'vue-i18n'
import Modal from '@/components/modal/index.vue'
import { NamespaceApiService, type Namespace } from '@/api/namespace-api-service'

// 国际化
const { t } = useI18n()

// 响应式数据
const loading = ref(false)
const error = ref('')
const success = ref('')
const namespaces = reactive<Namespace[]>([])
const selectedNamespace = ref<Namespace | null>(null)
const showModal = ref(false)
const showDeleteModal = ref(false)

// 新建Namespace表单数据
const newNamespace = reactive<Omit<Namespace, 'id'>>({
  name: '',
  code: '',
  description: '',
})

// 消息提示
const showMessage = (msg: string, type: 'success' | 'error') => {
  if (type === 'success') {
    success.value = msg
    setTimeout(() => { success.value = '' }, 3000)
  } else {
    error.value = msg
    setTimeout(() => { error.value = '' }, 5000)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const loadedNamespaces = await NamespaceApiService.getAllNamespaces()
    const normalizedNamespaces = loadedNamespaces.map(namespace => ({
      ...namespace
    }))

    namespaces.splice(0, namespaces.length, ...normalizedNamespaces)

    // 选中第一个Namespace
    if (normalizedNamespaces.length > 0) {
      await selectNamespace(normalizedNamespaces[0])
    }
  } catch (err: any) {
    console.error('加载数据失败:', err)
    showMessage(t('config.namespaceConfig.loadDataFailed') + ': ' + err.message, 'error')
  } finally {
    loading.value = false
  }
}

// 选择Namespace
const selectNamespace = async (namespace: Namespace) => {
  try {
    // 加载详细信息
    const detailedNamespace = await NamespaceApiService.getNamespaceById(namespace.id)
    selectedNamespace.value = {
      ...detailedNamespace
    }
  } catch (err: any) {
    console.error('加载Namespace详情失败:', err)
    showMessage(t('config.namespaceConfig.loadDetailsFailed') + ': ' + err.message, 'error')
    // 使用基本信息作为后备
    selectedNamespace.value = {
      ...namespace
    }
  }
}

// 显示新建Namespace弹窗
const showAddNamespaceModal = () => {
  newNamespace.name = ''
  newNamespace.code = ''
  newNamespace.description = ''
  showModal.value = true
}

// 创建新Namespace
const handleAddNamespace = async () => {
  if (!newNamespace.name.trim() || !newNamespace.code.trim() || !newNamespace.description.trim()) {
    showMessage(t('config.namespaceConfig.requiredFields'), 'error')
    return
  }

  try {
    const namespaceData: Omit<Namespace, 'id'> = {
      name: newNamespace.name.trim(),
      code: newNamespace.code.trim(),
      description: newNamespace.description.trim()
    }

    const createdNamespace = await NamespaceApiService.createNamespace(namespaceData)
    namespaces.push(createdNamespace)
    selectedNamespace.value = createdNamespace
    showModal.value = false
    showMessage(t('config.namespaceConfig.createSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.namespaceConfig.createFailed') + ': ' + err.message, 'error')
  }
}

// 保存Namespace
const handleSave = async () => {
  if (!selectedNamespace.value) return

  if (!selectedNamespace.value.name.trim() || !selectedNamespace.value.code.trim() || !selectedNamespace.value.description.trim()) {
    showMessage(t('config.namespaceConfig.requiredFields'), 'error')
    return
  }

  try {
    const savedNamespace = await NamespaceApiService.updateNamespace(selectedNamespace.value.id, selectedNamespace.value)

    // 更新本地列表中的数据
    const index = namespaces.findIndex(a => a.id === savedNamespace.id)
    if (index !== -1) {
      namespaces[index] = savedNamespace
    }

    selectedNamespace.value = savedNamespace
    showMessage(t('config.namespaceConfig.saveSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.namespaceConfig.saveFailed') + ': ' + err.message, 'error')
  }
}

// 显示删除确认
const showDeleteConfirm = () => {
  showDeleteModal.value = true
}

// 删除Namespace
const handleDelete = async () => {
  if (!selectedNamespace.value) return

  try {
    await NamespaceApiService.deleteNamespace(selectedNamespace.value.id)

    // 从列表中移除
    const index = namespaces.findIndex(a => a.id === selectedNamespace.value!.id)
    if (index !== -1) {
      namespaces.splice(index, 1)
    }

    // 选择其他Namespace或清除选中状态
    selectedNamespace.value = namespaces.length > 0 ? namespaces[0] : null
    showDeleteModal.value = false
    showMessage(t('config.namespaceConfig.deleteSuccess'), 'success')
  } catch (err: any) {
    showMessage(t('config.namespaceConfig.deleteFailed') + ': ' + err.message, 'error')
  }
}



// 组件挂载时加载数据
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.config-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.08));
  border-radius: 12px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.02);
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}



.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(102, 126, 234, 0.1);
  border: 1px solid rgba(102, 126, 234, 0.2);
  border-radius: 6px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.action-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.2);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.primary {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
}

.action-btn.danger {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.2);
}

.namespace-layout {
  flex: 1;
  display: flex;
  min-height: 0;
}

.namespace-list {
  width: 350px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.02);
}

.list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.namespace-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.namespaces-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.namespace-card {
  padding: 16px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.namespace-card:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(102, 126, 234, 0.3);
}

.namespace-card.active {
  background: rgba(102, 126, 234, 0.1);
  border-color: rgba(102, 126, 234, 0.4);
}

.namespace-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.namespace-name {
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.namespace-desc {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.4;
}

.namespace-code {
  display: flex;
  gap: 8px;
}

.namespace-tag {
  padding: 2px 8px;
  background: rgba(102, 126, 234, 0.2);
  border-radius: 4px;
  font-size: 12px;
  color: #667eea;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: rgba(255, 255, 255, 0.6);
}

.loading-icon, .empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 16px;
  padding: 12px;
  background: rgba(102, 126, 234, 0.1);
  border: 2px dashed rgba(102, 126, 234, 0.3);
  border-radius: 8px;
  color: #667eea;
  cursor: pointer;
  transition: all 0.3s;
}

.add-btn:hover {
  background: rgba(102, 126, 234, 0.15);
  border-color: rgba(102, 126, 234, 0.5);
}

.namespace-detail {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.detail-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.detail-actions {
  display: flex;
  gap: 12px;
}

.form-item {
  margin-bottom: 20px;
}

.form-item label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.9);
}

.required {
  color: #ef4444;
}

.form-item input,
.form-item textarea {
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  transition: all 0.3s;
}

.form-item input:focus,
.form-item textarea:focus {
  outline: none;
  border-color: rgba(102, 126, 234, 0.5);
  background: rgba(255, 255, 255, 0.08);
}

.form-item input::placeholder,
.form-item textarea::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.no-selection {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.6);
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.delete-confirm {
  text-align: center;
  padding: 20px 0;

  p {
    color: rgba(255, 255, 255, 0.8);
    margin: 8px 0;
  }

  .warning-text {
    color: rgba(255, 255, 255, 0.6);
    font-size: 14px;
  }
}

.warning-icon {
  font-size: 48px;
  color: #ffa726;
  margin-bottom: 16px;
}

.confirm-btn, .cancel-btn {
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;

  &.danger {
    background: rgba(234, 102, 102, 0.2);
    border: 1px solid rgba(234, 102, 102, 0.3);
    color: #ff8a8a;

    &:hover {
      background: rgba(234, 102, 102, 0.3);
    }
  }
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

.error-toast,
.success-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 6px;
  color: white;
  cursor: pointer;
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.error-toast {
  background: rgba(239, 68, 68, 0.9);
}

.success-toast {
  background: rgba(34, 197, 94, 0.9);
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>

