/**
 * 
 */
package cd.strommq.tcp;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * @author jinyu
 *
 */
public class MSGDecoder extends ByteToMessageDecoder{

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		//int num=in.readableBytes();
		out.add(in.array());
	}

}
