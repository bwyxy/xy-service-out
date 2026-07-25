export interface DayName {
    chineseFullName: string
    chineseShortName: string
    fullName: string
    shortName: string
    dayNumber: number
}

export interface CalendarProp {
    target: {
        year: number,
        month: number,
        date: number,
        day: number,
        monthShortName: string
        monthFullName: string
        monthChineseFullName: string
        firstDay: number,
        firstDayIndex: number,
        totalDays: number,
    },
    previous: {
        totalDays: number
    }
}

export interface CalendarData {
    /** 日期对象 */
    dateObj: Date
    /** 日 */
    dateNumber: number
    /**
     * 日期类型
     * previous - 上个月日期 | next - 下个月日期 ｜ current - 本月日期
     * */
    dateType: ('previous' | 'next' | 'current')
}
/**
 * 中国名字月份名集合
 * */
const chineseMonthName :string[] = ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"]
/**
 * 月份名集合
 * */
const monthName :string[] = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
export const dayNameList :DayName[] = [
    {
        chineseFullName: "周日",
        chineseShortName: "日",
        fullName: "Sunday",
        shortName: "Sun",
        dayNumber: 0,
    },
    {
        chineseFullName: "周一",
        chineseShortName: "一",
        fullName: "Monday",
        shortName: "Mon",
        dayNumber: 1,
    },
    {
        chineseFullName: "周二",
        chineseShortName: "二",
        fullName: "Tuesday",
        shortName: "Tue",
        dayNumber: 2,
    },
    {
        chineseFullName: "周三",
        chineseShortName: "三",
        fullName: "Wednesday",
        shortName: "Wed",
        dayNumber: 3,
    },
    {
        chineseFullName: "周四",
        chineseShortName: "四",
        fullName: "Thursday",
        shortName: "Thu",
        dayNumber: 4,
    },
    {
        chineseFullName: "周五",
        chineseShortName: "五",
        fullName: "Friday",
        shortName: "Fri",
        dayNumber: 5,
    },
    {
        chineseFullName: "周六",
        chineseShortName: "六",
        fullName: "Saturday",
        shortName: "Sat",
        dayNumber: 6,
    }
]

export function getCalendar(modelValue :Date) :{ calendarProps: CalendarProp, originData: CalendarData[][] } {
    const calendarProps :CalendarProp = {
        target: {
            year: 0,
            month: 0,
            date: 0,
            day: 0,
            monthShortName: '',
            monthFullName: '',
            monthChineseFullName: '',
            firstDay: 0,
            firstDayIndex: 0,
            totalDays: 0
        },
        previous: {
            totalDays: 0
        }
    }
    // 获取目标日期的年月日星期几数据
    const year = modelValue.getFullYear();
    const month = modelValue.getMonth();
    const date = modelValue.getDate();
    const day = modelValue.getDay();

    calendarProps.target.year = year
    calendarProps.target.month = month
    calendarProps.target.date = date
    calendarProps.target.day = day

    // 获取到目标日期的月份【中文】名称
    let dateString = modelValue.toString().split(" ");
    calendarProps.target.monthShortName = dateString[1];
    calendarProps.target.monthFullName = monthName[month];
    calendarProps.target.monthChineseFullName = chineseMonthName[month];
    // 获取目标月份的第一天是星期几，和在星期几中的索引值
    const targetMonthFirstDay = new Date(year, month, 1);
    calendarProps.target.firstDay = targetMonthFirstDay.getDay();
    calendarProps.target.firstDayIndex = dayNameList.findIndex(
        (day) => day.dayNumber === calendarProps.target.firstDay
    );

    // 获取目标月份总共多少天
    const targetMonthLastDay = new Date(year, month + 1, 0);
    calendarProps.target.totalDays = targetMonthLastDay.getDate();

    // 获取目标月份的上个月总共多少天
    const previousMonth = new Date(year, month, 0);
    calendarProps.previous.totalDays = previousMonth.getDate();

    let i;
    let dates = 1;
    const originData :CalendarData[][] = [];
    const firstRow :CalendarData[] = [];

    for (i = 0; i <= 6; i++) {
        if (i < calendarProps.target.firstDayIndex) {
            const previousDate = calendarProps.previous.totalDays - calendarProps.target.firstDayIndex + (i + 1);

            const dateObjs = new Date(calendarProps.target.year, calendarProps.target.month - 1, previousDate)

            firstRow.push({
                dateObj: dateObjs,
                dateNumber: previousDate,
                dateType: "previous",
            });
        } else {
            const dateObjs = new Date(calendarProps.target.year, calendarProps.target.month, dates)

            // 设置目标月份当月的日期数据
            firstRow.push({
                dateObj: dateObjs,
                dateNumber: dates,
                dateType: "current",
            });
            dates++;
        }
    }
    originData.push(firstRow);

    for (let j = 0; j <= 4; j++) {
        const rowData :CalendarData[] = [];
        for (let k = 0; k <= 6; k++) {
            if (dates <= calendarProps.target.totalDays) {
                const dateObjs = new Date(calendarProps.target.year, calendarProps.target.month, dates)

                rowData.push({
                    dateObj: dateObjs,
                    dateNumber: dates,
                    dateType: "current",
                });
            } else {
                // 设置目标月份下个月的日期数据
                const nextDate = dates - calendarProps.target.totalDays;

                const dateObjs = new Date(calendarProps.target.year, calendarProps.target.month + 1, nextDate)

                rowData.push({
                    dateObj: dateObjs,
                    dateNumber: nextDate,
                    dateType: "next",
                });
            }
            dates++;
        }
        originData.push(rowData);
    }

    return {
        calendarProps,
        originData
    }
}
