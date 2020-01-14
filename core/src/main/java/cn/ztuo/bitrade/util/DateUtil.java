package cn.ztuo.bitrade.util;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.util.Assert;

public class DateUtil {
    public static final DateFormat YYYY_MM_DD_MM_HH_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final DateFormat HHMMSS = new SimpleDateFormat("HH:mm:ss");
    public static final DateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");

    public static final DateFormat YYYYMMDDMMHHSSSSS = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    public static final DateFormat YYYY_MM_DD_MM_HH_SS_SSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static final DateFormat YYYYMMDDMMHHSSSS = new SimpleDateFormat("yyyyMMddHHmmssSS");

    public static final DateFormat YYYYMMDDHHMMSS = new SimpleDateFormat("yyyyMMddHHmmss");

    public static final DateFormat YYYYMMDD = new SimpleDateFormat("yyyyMMdd");

    public static String dateToString(Date date) {
        return YYYY_MM_DD_MM_HH_SS.format(date);
    }

    public static String dateToStringDate(Date date) {
        return YYYY_MM_DD.format(date);
    }

	public static String dateToStringLengthIs17(Date date) {
		return YYYYMMDDMMHHSSSSS.format(date);
	}

    public static String dateTo3S(Date date){
        return YYYY_MM_DD_MM_HH_SS_SSS.format(date);
    }

    public static Calendar calendar(Date date) {
       return calendar(date);
    }
    public static Date getNowEndTime() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR_OF_DAY, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTime();
    }
    public static Date getNowStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR_OF_DAY, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTime();
    }
    public static Calendar beginOfDay(Calendar calendar) {
        calendar.set(11, 0);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar;
    }

    public static Calendar endOfDay(Calendar calendar) {
        calendar.set(11, 23);
        calendar.set(12, 59);
        calendar.set(13, 59);
        calendar.set(14, 999);
        return calendar;
    }
    /**
     * 开始时间 结束时间 是否合法  // 判断是否开始时间小于今天并且开始时间小于结束时间
     *
     * @param startDate
     * @param endDate
     */
    public static void validateDate(Date startDate, Date endDate) {
        Date currentDate = DateUtil.getCurrentDate();
        int compare = compare(startDate, currentDate);
        int compare2 = compare(startDate, endDate);
        Assert.isTrue(compare != -1, "startDate cannot be less than currentDate!");
        Assert.isTrue(compare2 != 1, "startDate must be less than endDate!");
    }

    public static void validateEndDate(Date endDate) {
        Date currentDate = DateUtil.getCurrentDate();
        int compare = compare(currentDate, endDate);
        Assert.isTrue(compare != 1, "currentDate must be less than endDate!");
    }

    /**
     * @param date1
     * @param date2
     * @return 1 大于 -1 小于 0 相等
     */
    public static int compare(Date date1, Date date2) {
        try {
            if (date1.getTime() > date2.getTime()) {
                return 1;
            } else if (date1.getTime() < date2.getTime()) {
                return -1;
            } else {
                return 0;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取当时日期时间串 格式 yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getDateTime() {
        return YYYY_MM_DD_MM_HH_SS.format(new Date());
    }


    public static Date getStringToDate3S(String dateString){
        try {
            return YYYYMMDDMMHHSSSSS.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date getStringToDate2S(String dateString){
        try {
            return YYYYMMDDMMHHSSSS.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当时日期串 格式 yyyy-MM-dd
     *
     * @return
     */
    public static String getDate() {
        return YYYY_MM_DD.format(new Date());
    }

    public static String getDateYMD() {
        return YYYYMMDD.format(new Date());
    }

    public static String getDateYMD(Date date) {
        return YYYYMMDD.format(date);
    }

    public static Date strToDate(String dateString) {
        Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date strToYYMMDDDate(String dateString) {
        Date date = null;
        try {
            date = YYYY_MM_DD.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static long diffDays(Date startDate, Date endDate) {
        long days = 0L;
        long start = startDate.getTime();
        long end = endDate.getTime();
        days = (end - start) / 86400000L;
        return days;
    }

    public static Date dateAddMonth(Date date, int month) {
        return add(date, 2, month);
    }

    public static Date dateAddDay(Date date, int day) {
        return add(date, 6, day);
    }

    public static Date dateAddYear(Date date, int year) {
        return add(date, 1, year);
    }

    public static String dateAddDay(String dateString, int day) {
        Date date = strToYYMMDDDate(dateString);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(6, day);
        return YYYY_MM_DD.format(calendar.getTime());
    }

    public static String dateAddDay(int day) {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(6, day);
        return YYYY_MM_DD.format(calendar.getTime());
    }

    public static String dateAddMonth(int month) {
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(2, month);
        return YYYY_MM_DD.format(calendar.getTime());
    }

    public static String remainDateToString(Date startDate, Date endDate) {
        StringBuilder result = new StringBuilder();
        if (endDate == null) {
            return "过期";
        }
        long times = endDate.getTime() - startDate.getTime();
        if (times < -1L) {
            result.append("过期");
        } else {
            long temp = 86400000L;

            long d = times / temp;

            times %= temp;
            temp /= 24L;
            long m = times / temp;

            times %= temp;
            temp /= 60L;
            long s = times / temp;

            result.append(d);
            result.append("天");
            result.append(m);
            result.append("小时");
            result.append(s);
            result.append("分");
        }
        return result.toString();
    }

    private static Date add(Date date, int type, int value) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(type, value);
        return calendar.getTime();
    }

    public static String getLinkUrl(boolean flag, String content, String id) {
        if (flag) {
            content = "<a href='finance.do?id=" + id + "'>" + content + "</a>";
        }
        return content;
    }

    public static long getTimeCur(String format, String date) throws ParseException {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        return sf.parse(sf.format(date)).getTime();
    }

    public static long getTimeCur(String format, Date date) throws ParseException {
        SimpleDateFormat sf = new SimpleDateFormat(format);
        return sf.parse(sf.format(date)).getTime();
    }

    public static String getStrTime(String cc_time) {
        String re_StrTime = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        long lcc_time = Long.valueOf(cc_time).longValue();
        re_StrTime = sdf.format(new Date(lcc_time * 1000L));
        return re_StrTime;
    }


    public static Date getCurrentDate() {
        return new Date();
    }

    public static String getFormatTime(DateFormat format, Date date) throws ParseException {
        return format.format(date);
    }

    /**
     * 获取时间戳
     *
     * @return
     */
    public static long getTimeMillis() {
        return System.currentTimeMillis();
    }

    public static String getWeekDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case 1:
                return "周日";
            case 2:
                return "周一";
            case 3:
                return "周二";
            case 4:
                return "周三";
            case 5:
                return "周四";
            case 6:
                return "周五";
            case 7:
                return "周六";
            default:
            return "";
        }
    }

    public static String toGMTString(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.UK);
        df.setTimeZone(new java.util.SimpleTimeZone(0, "GMT"));
        return df.format(date);
    }

    /**
     * 得到当前时间与某个时间的差的分钟数
     *
     * @param date
     * @return
     */
    public static BigDecimal diffMinute(Date date) {
        return BigDecimalUtils.div(new BigDecimal(System.currentTimeMillis() - date.getTime()), new BigDecimal("60000"));
    }

    /**
     * 获取过去第几天的日期
     *
     * @param past
     * @return
     */
    public static String getPastDate(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - past);
        Date today = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String result = format.format(today);
        return result;
    }

    /**
     * 获取未来 第 past 天的日期
     *
     * @param past
     * @return
     */
    public static String getFetureDate(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + past);
        Date today = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String result = format.format(today);
        return result;
    }

    public static int getDatePart(Date date,int part) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(part);
    }

    public static Date getDate(Date date , int day){

        synchronized (YYYY_MM_DD){
            Calendar calendar = Calendar.getInstance() ;
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH,-day);
            date = calendar.getTime() ;
            try {
                return YYYY_MM_DD.parse(YYYY_MM_DD.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
    
    public static String getDateRandom() {
    	return DateFormatUtils.format(new Date(), "yyyyMMddHHmmss")+(int)((Math.random()*9+1)*10000);
    }
    
    public static Date getDateNoTime(Date curDate,int amount) {
    	Calendar cal = Calendar.getInstance();
		cal.setTime(curDate);
		cal.add(Calendar.DAY_OF_MONTH, amount);
    	cal.set(Calendar.HOUR_OF_DAY, 0);
    	cal.set(Calendar.MINUTE, 0);
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
    }

    public static Date addMinute(Integer minute){
        long currentTime = System.currentTimeMillis() + minute * 60 * 1000;
        Date date = new Date(currentTime);
        return date;
    }

    public static Date addMinute(String date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(sdf.format(date));
        Date date1= null;
        try {
            date1 = sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return date1;
    }

    public static Boolean compareTime(){
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();
        System.out.println(hour);
        System.out.println(minute);
        if(hour>=23 && minute>30){
            return true;
        }
        return false;
    }

    /**
     * 计算2个日期之间相差的  相差多少日
     * 比如：2011-02-02 到  2017-03-02 相差 6年，1个月，0天
     * @param fromDate
     * @param toDate
     * @return
     */
    public static int dayComparePrecise(Date fromDate,Date toDate){
        Calendar  from  =  Calendar.getInstance();
        from.setTime(fromDate);
        Calendar  to  =  Calendar.getInstance();
        to.setTime(toDate);
        int fromDay = from.get(Calendar.DAY_OF_MONTH);
        int toDay = to.get(Calendar.DAY_OF_MONTH);
        int day = toDay  - fromDay;
        return day;
    }

    public static int differentDays(Date date1,Date date2)
    {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        int day1= cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);
        if(year1 != year2)   //同一年
        {
            int timeDistance = 0 ;
            for(int i = year1 ; i < year2 ; i ++)
            {
                if(i%4==0 && i%100!=0 || i%400==0)    //闰年
                {
                    timeDistance += 366;
                }
                else    //不是闰年
                {
                    timeDistance += 365;
                }
            }

            return timeDistance + (day2-day1) ;
        }
        else    //不同年
        {
            System.out.println("判断day2 - day1 : " + (day2-day1));
            return day2-day1;
        }
    }
}