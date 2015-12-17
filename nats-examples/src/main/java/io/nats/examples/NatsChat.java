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
import java.io.IOException;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.util.Base64;

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

  private static String read_line (String prompt)
    throws IOException
  {
    String res;

    java.io.Console cons = System.console ();
    while (true)
      {
        System.out.print (prompt);
        System.out.flush ();
        if (cons != null)
          res = cons.readLine ();
        else
          res = new java.io.BufferedReader (new java.io.InputStreamReader (System.in)).readLine ();

        if (res != null)
          {
            // ignore any blank lines, trim off white space...
            res = res.trim ();
            if (res.length () == 0)
              continue;
          }
        return res;
      }
  }
  public static void main (String args[])
    throws Exception
    {
      URI uri = new URI ("nats://demo.nats.io:4443");

      if (args.length != 2)
        {
          System.err.format ("usage: %s subject key%n", NatsChat.class.getSimpleName ());
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

      final String my_name = read_line ("Your name: ");

      try
	{
          final Context ctx = new io.nats.imp.Context ();
          final Options opts = new Options ();

          opts.secure = true;
          opts.name = "nats-chat";
          opts.servers.add (uri);

          final Connection conn = ctx.connect (opts);
          final Destination destination = conn.destination (String.format ("snats.%s.%s", PROTO, subject));
          final ByteBuf msg_body = ByteBufAllocator.DEFAULT.heapBuffer ();

	  System.out.println ("connected to nats secure chat server @ " + conn);

          conn.subscribe (destination, new MessageHandler<ByteBuf> () {
            private volatile int msgNum = 0;

            public void onMessage (Subscription subscription,
                                   Destination subject,
                                   Destination reply,
                                   ByteBuf body)
            {
              final int n = ++msgNum;
              // FIXME: jam: decode the json object from the body and decrypt only the (base64) encrpted_msg field
              final byte raw_mesg[] = new byte[body.readableBytes ()];
              body.readBytes (raw_mesg);
              body.clear ();
              try {
                final byte decrypted[] = decryptor.doFinal (raw_mesg);
                body.writeBytes (decrypted);
              }
              catch (IllegalBlockSizeException ibe)
              {
                body.writeBytes (String.format ("unable to decrypt message: %s", ibe.getMessage ()).getBytes ());
              }
              catch (BadPaddingException pbe)
              {
                body.writeBytes (String.format ("unable to decrypt message: %s", pbe.getMessage ()).getBytes ());
              }

              System.out.format ("%5s: received message on %s: %s%n",
                                 n, subject, body.toString (CharsetUtil.US_ASCII));
              System.out.flush ();
            }
          });
          while (true)
            {
              final String line = read_line (String.format ("%s> ", my_name));
              if (line == null)
                break;
              if (line.equals ("bye") || line.equals ("exit")) break;
              byte encrypted[] = encryptor.doFinal (line.getBytes (CharsetUtil.UTF_8));
              byte encoded[] = Base64.getEncoder ().encode (encrypted);
              final String encoded_str = new String (encoded);

              msg_body.clear ();
              msg_body.writeBytes (String.format ("{ \"name\": \"%s\", \"encrpyed_msg\": \"%s\" }",
                                                  my_name,
                                                  encoded_str).getBytes (CharsetUtil.US_ASCII));
              conn.publish (destination, null, msg_body.duplicate ());
            }
          conn.close ();
        }
      catch (NatsException ex)
	{
	  System.err.println ("caught nats exception: " + ex);
	  ex.printStackTrace (System.err);
	  System.exit (1);
	}
    }
}