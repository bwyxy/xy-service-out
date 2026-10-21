#!/usr/bin/env node
/**
 * 清理脚本
 * 支持清理 node_modules、dist、tsbuildinfo、pnpm store 等缓存
 * 用法：
 *   node scripts/clean.js           默认清理 node_modules + dist + tsbuildinfo
 *   node scripts/clean.js --all     额外清理 pnpm store 和各种临时缓存
 *   node scripts/clean.js --deps    只清理 node_modules
 *   node scripts/clean.js --dist    只清理 dist
 *   node scripts/clean.js --cache   只清理 pnpm store 和缓存文件
 */
(function () {
  const fs = require('fs')
  const path = require('path')
  const { execSync } = require('child_process')

  const rootDir = path.resolve(__dirname, '..')
  const args = process.argv.slice(2)

  if (args.includes('--help') || args.includes('-h')) {
    console.log(`
用法：node scripts/clean.js [选项]

选项：
  --all     清理 node_modules + dist + tsbuildinfo + pnpm store + 各类缓存
  --deps    只清理 node_modules
  --dist    只清理 dist + tsbuildinfo
  --cache   只清理 pnpm store + 各类缓存
  --dry-run 只打印将要清理的内容，不实际删除
  --help    显示帮助信息

默认（无参数）会清理 node_modules + dist + tsconfig.tsbuildinfo。
`)
    process.exit(0)
  }

  const flagDryRun = args.includes('--dry-run')
  const flagAll = args.includes('--all')
  const flagDeps = args.includes('--deps') || flagAll
  const flagDist = args.includes('--dist') || flagAll
  const flagCache = args.includes('--cache') || flagAll

// 没有任何参数时走默认行为：清理 node_modules + dist + tsbuildinfo
  const isDefault = !args.length || (!flagDeps && !flagDist && !flagCache)
  const shouldCleanDeps = isDefault || flagDeps
  const shouldCleanDist = isDefault || flagDist
  const shouldCleanCache = flagCache

  const patterns = {
    nodeModules: 'node_modules',
    dist: 'dist',
    tsbuildinfo: 'tsconfig.tsbuildinfo',
  }

  function readdir(dir) {
    if (!fs.existsSync(dir)) return []
    return fs.readdirSync(dir, { withFileTypes: true })
  }

  function remove(target) {
    if (!fs.existsSync(target)) return false
    const stat = fs.statSync(target)
    const type = stat.isDirectory() ? '目录' : '文件'
    const relative = path.relative(rootDir, target)
    if (flagDryRun) {
      console.log(`  [预览] 将删除 ${type}: ${relative}`)
      return true
    }
    fs.rmSync(target, { recursive: true, force: true })
    console.log(`  已删除 ${type}: ${relative}`)
    return true
  }

  function walkWorkspace(baseDir, cb) {
    readdir(baseDir).forEach(entry => {
      if (!entry.isDirectory()) return
      const projectDir = path.join(baseDir, entry.name)
      const pkgPath = path.join(projectDir, 'package.json')
      if (fs.existsSync(pkgPath)) cb(projectDir, entry.name)
    })
  }

  function collectTargets() {
    const targets = []

    if (shouldCleanDeps) {
      targets.push(path.join(rootDir, patterns.nodeModules))
      walkWorkspace(path.join(rootDir, 'apps'), dir => targets.push(path.join(dir, patterns.nodeModules)))
      walkWorkspace(path.join(rootDir, 'packages'), dir => targets.push(path.join(dir, patterns.nodeModules)))
    }

    if (shouldCleanDist) {
      walkWorkspace(path.join(rootDir, 'apps'), dir => targets.push(path.join(dir, patterns.dist)))
      walkWorkspace(path.join(rootDir, 'packages'), dir => targets.push(path.join(dir, patterns.dist)))
    }

    if (shouldCleanDist || isDefault) {
      // 清理各项目下的 tsconfig.tsbuildinfo
      walkWorkspace(path.join(rootDir, 'apps'), dir => targets.push(path.join(dir, patterns.tsbuildinfo)))
      walkWorkspace(path.join(rootDir, 'packages'), dir => targets.push(path.join(dir, patterns.tsbuildinfo)))
    }

    if (shouldCleanCache) {
      const cacheDirs = ['.cache', '.turbo', '.vite', '.parcel-cache', '.eslintcache', '.stylelintcache']
      targets.push(path.join(rootDir, '.pnpm-store'))
      targets.push(path.join(rootDir, '.pnpm'))
      walkWorkspace(path.join(rootDir, 'apps'), dir => {
        cacheDirs.forEach(c => targets.push(path.join(dir, c)))
      })
      walkWorkspace(path.join(rootDir, 'packages'), dir => {
        cacheDirs.forEach(c => targets.push(path.join(dir, c)))
      })
    }

    return targets
  }

  function main() {
    console.log('🧹 开始清理...')

    const targets = collectTargets()
    let deletedCount = 0

    targets.forEach(target => {
      if (remove(target)) deletedCount++
    })

    if (shouldCleanCache) {
      if (flagDryRun) {
        console.log('  [预览] 将执行 pnpm store prune')
      } else {
        try {
          console.log('  清理 pnpm store...')
          execSync('pnpm store prune', { stdio: 'inherit' })
        } catch (err) {
          console.warn('  ⚠️  pnpm store prune 执行失败，可能 pnpm 未安装或 store 为空')
        }
      }
    }

    if (deletedCount === 0 && !shouldCleanCache) {
      console.log('✅ 没有发现需要清理的内容')
    } else {
      console.log(`✅ 清理完成，共删除 ${deletedCount} 个目录/文件`)
    }
  }

  main()
})()
