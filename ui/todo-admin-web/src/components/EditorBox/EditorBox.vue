<script setup>
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import {onBeforeUnmount, reactive, shallowRef} from "vue";

const editor = shallowRef(null)
const content = defineModel('modelValue', {
  type: String,
  default: ''
})

const defaultToolbarConfig = reactive({
  toolbarKeys: [
    'bold',
    'italic',
    'underline',
    'through',
    'fontFamily',   // 如果报错，删除
    'fontSize',
    'color',
    'bgColor',
    'headerSelect',
    'blockquote',
    'codeBlock',
    'insertLink',
    'insertImage',
    'insertTable',
    'bulletedList',
    'numberedList',
    'xy',         // 如果报错，删除
    'indent',
    'undo',
    'redo',
    'uploadImage'
  ],
  modalAppendToBody: false,
  excludeKeys: []
})

const defaultEditorConfig = reactive({
  placeholder: '请输入...',
  hoverbarKeys: {
    image: {
      menuKeys: ['imageWidth30', 'imageWidth50', 'imageWidth100', 'editImage', 'viewImageLink', 'deleteImage'],
    },
  }
})

const createEditor = (editorVal) => {
  editor.value = editorVal
}

onBeforeUnmount(() => {
  if (!editor.value) return
  editor.value.destroy()
})
</script>

<template>
  <div class="editor-box">
    <Toolbar
        :editor="editor"
        :defaultConfig="defaultToolbarConfig"
        :style="{borderBottom: '1px solid #f0f0f0'}"
    />

    <Editor
        v-model="content"
        @onCreated="createEditor"
        :defaultConfig="defaultEditorConfig"
        style="height: 300px"
    />
  </div>
</template>

<style scoped lang="scss">
.editor-box {
  width: 100%;
  border: 1px solid #f0f0f0;
}
</style>
