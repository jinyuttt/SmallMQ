/**
 * 
 */
package cd.strommq.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author jinyu
 *
 */
public class MSGEncoder  extends MessageToByteEncoder<byte[]>{

	

	@Override
	protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception {
     
		
	}

}
