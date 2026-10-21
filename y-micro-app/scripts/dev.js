#!/usr/bin/env node
/**
 * 通用启动脚本
 * 支持交互式选择项目，或通过参数直接指定项目
 */
(async function () {
  const fs = require('fs')
  const path = require('path')
  const readline = require('readline')
  const { execSync, spawn } = require('child_process')

  const rootDir = path.resolve(__dirname, '..')
  const appsDir = path.join(rootDir, 'apps')

  function parseArgs(argv) {
    let project = null
    let mode = 'development'

    for (let i = 0; i < argv.length; i++) {
      const arg = argv[i]
      if (arg.startsWith('--mode=')) {
        mode = arg.split('=')[1]
      } else if (arg === '--mode') {
        mode = argv[i + 1]
        i++
      } else if (!arg.startsWith('-') && !project) {
        project = arg
      }
    }

    return { project, mode }
  }

  function printHelp() {
    console.log(`
用法：node scripts/dev.js [选项] [项目简称]

选项：
  --mode <development|production>  指定运行模式，默认 development
  --help, -h                       显示帮助

示例：
  node scripts/dev.js                          交互式选择项目
  node scripts/dev.js monitor                  开发模式启动 monitor
  node scripts/dev.js monitor --mode production  生产模式启动 monitor
  node scripts/dev.js --mode production screen  生产模式启动 screen
`)
  }

  function getProjects() {
    if (!fs.existsSync(appsDir)) return []
    return fs.readdirSync(appsDir, { withFileTypes: true })
        .filter(entry => entry.isDirectory())
        .map(entry => {
          const dir = path.join(appsDir, entry.name)
          const pkgPath = path.join(dir, 'package.json')
          const pkg = fs.existsSync(pkgPath) ? JSON.parse(fs.readFileSync(pkgPath, 'utf-8')) : {}
          return {
            name: entry.name,
            dir,
            pkgName: pkg.name || entry.name
          }
        })
        .filter(p => fs.existsSync(path.join(p.dir, 'package.json')))
  }

  function findProjects(input) {
    const projects = getProjects()
    const lowerInput = input.toLowerCase()

    const exact = projects.find(p => p.name.toLowerCase() === lowerInput)
    if (exact) return [exact]

    const prefixMatch = projects.filter(p =>
        p.name.toLowerCase() === `hzy-${lowerInput}-sub` ||
        p.name.toLowerCase() === `hzy-${lowerInput}-web` ||
        p.name.toLowerCase() === `hzy-${lowerInput}-cop` ||
        p.name.toLowerCase() === `hzy-${lowerInput}`
    )
    if (prefixMatch.length) return prefixMatch

    const contains = projects.filter(p => p.name.toLowerCase().includes(lowerInput))
    if (contains.length) return contains

    return projects.filter(p => p.pkgName.toLowerCase().includes(lowerInput))
  }

  function selectProject(projects) {
    return new Promise((resolve, reject) => {
      console.log('\n请选择要启动的项目：')
      projects.forEach((p, index) => {
        console.log(`  ${index + 1}. ${p.name} (${p.pkgName})`)
      })
      console.log('  0. 取消')

      const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
      })

      rl.question('\n输入序号: ', answer => {
        rl.close()
        const num = parseInt(answer.trim(), 10)
        if (Number.isNaN(num) || num < 0 || num > projects.length) {
          reject(new Error('无效的选择'))
          return
        }
        if (num === 0) {
          reject(new Error('已取消'))
          return
        }
        resolve(projects[num - 1])
      })
    })
  }

  async function main() {
    const args = process.argv.slice(2)

    if (args.includes('--help') || args.includes('-h')) {
      printHelp()
      process.exit(0)
    }

    const { project: input, mode } = parseArgs(args)
    const isProduction = mode === 'production'
    const hookScript = isProduction ? 'prebuild' : 'predev'
    const runScript = isProduction ? 'build' : 'dev'

    let target

    if (input) {
      const matches = findProjects(input)
      if (matches.length === 0) {
        const all = getProjects().map(p => p.name).join(', ')
        console.error(`❌ 未找到与 "${input}" 匹配的项目`)
        console.error(`可用项目：${all}`)
        process.exit(1)
      }
      if (matches.length > 1) {
        console.error(`❌ "${input}" 匹配到多个项目，请使用更精确的名称：`)
        matches.forEach(p => console.error(`  - ${p.name} (${p.pkgName})`))
        process.exit(1)
      }
      target = matches[0]
    } else {
      const projects = getProjects()
      if (projects.length === 0) {
        console.error('❌ 未找到任何 apps 项目')
        process.exit(1)
      }
      try {
        target = await selectProject(projects)
      } catch (err) {
        console.log(err.message)
        process.exit(1)
      }
    }

    console.log(`🚀 启动项目：${target.name} (${target.pkgName})，模式：${mode}`)

    // 检查根目录是否存在 predev/prebuild 脚本，不存在则跳过
    const rootPkg = JSON.parse(fs.readFileSync(path.join(rootDir, 'package.json'), 'utf-8'))
    const hasHookScript = rootPkg.scripts && rootPkg.scripts[hookScript]
    // 检查安装依赖
    execSync(`pnpm --filter ./apps/${target.name} install`)
    // 打包公共资源包
    execSync("pnpm run install:packages && pnpm run build:packages")
    if (hasHookScript) {
      try {
        execSync(`pnpm run ${hookScript}`, { stdio: 'inherit', cwd: rootDir })
      } catch (err) {
        console.error(`❌ ${hookScript} 执行失败`)
        process.exit(1)
      }
    } else {
      console.warn(`⚠️ 根目录 package.json 缺少 "${hookScript}" 脚本，已跳过`)
    }

    const child = spawn('pnpm', [
      '--filter',
      `./apps/${target.name}`,
      'run',
      runScript,
      '--mode',
      mode
    ], {
      stdio: 'inherit',
      cwd: rootDir,
      shell: true
    })

    child.on('exit', code => {
      process.exit(code)
    })
  }

  await main()
})()
