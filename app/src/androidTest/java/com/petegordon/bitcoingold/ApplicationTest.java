package com.petegordon.bitcoingold;

import android.app.Application;
import android.content.Intent;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.Button;

import com.petegordon.bitcoingold.data.BitcoinGoldContract;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    @MediumTest
    public void testNextActivityWasLaunchedWithIntent() {
        //Default is 10 days worth of data
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.roll(Calendar.DATE, -(BitcoinGoldContract.NUMBER_OF_DAYS+1));
        Date startDate = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strStartDate = dateFormat.format(startDate);
        System.out.println(strStartDate);

        Date d = new Date();//intialize your date to any date
        Date dateBefore = new Date(new Date().getTime() - (BitcoinGoldContract.NUMBER_OF_DAYS+1) * 86400000 ); //Subtract n days
        String strStartDate2 = dateFormat.format(dateBefore);
        System.out.println(strStartDate2);
    }
}