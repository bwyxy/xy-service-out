import { formatDateTime } from './formatDateTime'

/** 根据开始时间 结束时间 推演每隔几分钟的时间
 *
 * */
export function generateHalfHourIntervals(
    startTime :string | Date,
    endTime :string | Date,
    time :number = 30,
    outTime:number = 30
) :string[] {
    const intervals :string[] = [];
    let startDateTime :number = 0
    let endDateTime :number = 0

    if (typeof startTime === 'string') {
        startDateTime = new Date(formatDateTime(startTime)).getTime()
    } else {
        startDateTime = new Date(startTime).getTime()
    }

    if (typeof endTime === 'string') {
        endDateTime = new Date(formatDateTime(endTime)).getTime()
    } else {
        endDateTime = new Date(endTime).getTime()
    }

    let nextCurrent = new Date(startDateTime + outTime * 60 * 1000)
    let prevTime = new Date(endDateTime + time * 60 * 1000)

    const minutes = nextCurrent.getMinutes()
    if (minutes > 0 && minutes < 30) {
        nextCurrent.setMinutes(minutes, 0, 0)
    } else if (minutes > 30) {
        nextCurrent.setHours(nextCurrent.getHours() + 1, 60 - minutes, 0, 0)
    } else {
        nextCurrent.setMinutes(minutes, 0, 0)
    }

    nextCurrent = getRuleTime(new Date(prevTime.getTime() + outTime * 60 * 1000))
    const end = new Date(endTime)

    // 以此递增
    while (nextCurrent <= end) {
        intervals.push(`${formatDateTime(prevTime, 'HH:mm')}~${formatDateTime(nextCurrent, 'HH:mm')}`)
        prevTime = getRuleTime(new Date(nextCurrent.getTime()))
        nextCurrent = getRuleTime(new Date(nextCurrent.getTime() + outTime * 60 * 1000))
    }
    return intervals
}

function getRuleTime(time :Date) :Date {
    let prevTime = time
    if(0 < prevTime.getMinutes() && prevTime.getMinutes() < 10) {
        prevTime = new Date(prevTime.setMinutes(0, 0, 0))
    }else if(10 <= prevTime.getMinutes() && prevTime.getMinutes() <= 40) {
        prevTime = new Date(prevTime.setMinutes(30, 0, 0))
    }else if(40 < prevTime.getMinutes() && prevTime.getMinutes() <= 60) {
        prevTime = new Date(prevTime.setHours(prevTime.getHours() + 1, 0, 0, 0))
    }

    return prevTime
}
