package edu.uw.spl;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import test.AccountManagerTest;
import test.AccountTest;
import test.BrokerTest;
import test.DaoTest;
import test.PrivateMessageCodecTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({PrivateMessageCodecTest.class})
public class TestSuite{
}