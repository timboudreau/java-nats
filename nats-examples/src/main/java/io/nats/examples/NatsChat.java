/*
 * Copyright 2015 Apcera Inc. All rights reserved.
 */

package io.nats.examples;

import io.nats.Connection;
import io.nats.Context;
import io.nats.Destination;
import io.nats.MessageHandler;
import io.nats.NatsException;
import io.nats.Options;
import io.nats.Subscription;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.CharsetUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.security.MessageDigest;

/**
 * Secure chat example in Java that follows the go example.
 *
 * Usage: SecureChat.main Subject Key
 *
 * @link https://github.com/derekcollison/nats_chat
 */
public class NatsChat
{
  static final private String PROTO = "1";

  public static void main (String args[])
    throws Exception
    {
      URI uri = Options.DEFAULT_URI;

      if (args.length != 2)
        {
          System.err.println ("".format ("usage: %s subject key", NatsChat.class.getSimpleName ()));
          System.exit (1);
        }

      final String subject = args[0];
      final String key = args[1];

      final String ALGO = "AES";
      final MessageDigest digester = MessageDigest.getInstance ("SHA-256");
      final byte block[] = digester.digest (key.getBytes ());
      final SecretKey secret_key = new SecretKeySpec (block, ALGO);
      final Cipher encryptor = Cipher.getInstance (ALGO);
      encryptor.init (Cipher.ENCRYPT_MODE, secret_key);
      final Cipher decryptor = Cipher.getInstance (ALGO);
      decryptor.init (Cipher.DECRYPT_MODE, secret_key);

      try
	{
          final Context ctx = new io.nats.imp.Context ();
          final Options opts = new Options ();

          opts.secure = true;
          opts.name = "nats-chat";
          opts.servers.add (uri);

          final Connection conn = ctx.connect (opts);
          final Destination destination = conn.destination ("".format ("snats.%s.%s", PROTO, subject));
          final ByteBuf msg_body = ByteBufAllocator.DEFAULT.heapBuffer ();

	  System.out.println ("connected to nats: " + conn);

          conn.subscribe (destination, new MessageHandler<ByteBuf> () {
            private volatile int msgNum = 0;

            public void onMessage (Subscription subscription,
                                   Destination subject,
                                   Destination reply,
                                   ByteBuf body)
            {
              final int n = ++msgNum;
              final byte raw_mesg[] = new byte[body.readableBytes ()];
              body.readBytes (raw_mesg);
              body.clear ();
              try {
                final byte decrypted[] = decryptor.doFinal (raw_mesg);
                body.writeBytes (decrypted);
              }
              catch (IllegalBlockSizeException ibe)
              {
                body.writeBytes ("".format ("unable to decrypt message: %s", ibe.getMessage ()).getBytes ());
              }
              catch (BadPaddingException pbe)
              {
                body.writeBytes ("".format ("unable to decrypt message: %s", pbe.getMessage ()).getBytes ());
              }

              System.out.printf ("%5s: received message on %s: %s\n",
                                 n, subject, body.toString (CharsetUtil.US_ASCII));
              System.out.flush ();
            }
          });
          /*
          int i;
          for (i = 0; i < 10; i++)
            {
              Destination dest = conn.destination (String.format (SUBSCRIBE_SUBJECT_FORMAT, i));
              conn.publish (dest, null, msg_body.duplicate ());
            }
          if (!subscribe)
            {
              System.out.println ("published " + i + " messages to " + subject);
              return;
            }
          final long done_pub_time = System.currentTimeMillis ();
          while (true)
            {
              final int how_many = msgNum;
              final int diff = i - how_many;
              final long now = System.currentTimeMillis ();
              final long delta_time = now - done_pub_time;
              //System.out.println ("td=" + delta_time + " diff=" + diff + " received=" + how_many + " sent=" + i);
              //System.out.flush ();
              if (diff == 0)
                {
                  System.out.println ("that took: " + TimeUnit.MILLISECONDS.toSeconds (delta_time)
                                      + " to publish " + i + " messages of " + msg_body.writerIndex () + " ("
                                      + (i * msg_body.writerIndex ()) + ")");
                  break;
                }
              Thread.sleep (TimeUnit.SECONDS.toMillis (1) / 200);
            }*/
        }
      catch (NatsException ex)
	{
	  System.err.println ("caught nats exception: " + ex);
	  ex.printStackTrace (System.err);
	  System.exit (1);
	}
    }
}