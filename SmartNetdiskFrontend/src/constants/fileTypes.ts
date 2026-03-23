import {
  Document, Folder, Picture, VideoPlay, Headset,
  FolderOpened, Files
} from '@element-plus/icons-vue'
import type { Component } from 'vue'

export interface FileTypeConfig {
  icon: Component
  color: string
  cssClass: string
  label: string
}

export const FILE_TYPE_MAP: Record<string, FileTypeConfig> = {
  folder: {
    icon: FolderOpened,
    color: 'var(--color-folder)',
    cssClass: 'type-folder',
    label: '文件夹'
  },
  image: {
    icon: Picture,
    color: 'var(--color-image)',
    cssClass: 'type-image',
    label: '图片'
  },
  video: {
    icon: VideoPlay,
    color: 'var(--color-video)',
    cssClass: 'type-video',
    label: '视频'
  },
  audio: {
    icon: Headset,
    color: 'var(--color-audio)',
    cssClass: 'type-audio',
    label: '音频'
  },
  document: {
    icon: Document,
    color: 'var(--color-document)',
    cssClass: 'type-document',
    label: '文档'
  },
  archive: {
    icon: Files,
    color: 'var(--color-archive)',
    cssClass: 'type-archive',
    label: '压缩包'
  },
  code: {
    icon: Document,
    color: 'var(--color-code)',
    cssClass: 'type-code',
    label: '代码'
  }
}

const DEFAULT_CONFIG: FileTypeConfig = {
  icon: Document,
  color: '#6B7280',
  cssClass: 'type-document',
  label: '文件'
}

export function getFileTypeConfig(type: string): FileTypeConfig {
  return FILE_TYPE_MAP[type] || DEFAULT_CONFIG
}

export function getFileIcon(type: string): Component {
  return getFileTypeConfig(type).icon
}

export function getFileIconColor(type: string): string {
  return getFileTypeConfig(type).color
}

export function getFileTypeClass(type: string): string {
  return getFileTypeConfig(type).cssClass
}

export function formatTime(time: string): string {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}
